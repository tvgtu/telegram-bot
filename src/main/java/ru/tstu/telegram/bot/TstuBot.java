package ru.tstu.telegram.bot;

import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.api.methods.send.SendInvoice;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import ru.tstu.telegram.dao.MessagesDAOService;
import ru.tstu.telegram.model.TelegramMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This example bot is an echo bot that just repeats the messages sent to him
 */
@Component
public class TstuBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TstuBot.class);

    @Value("${bot.token}")
    private String token;

    @Value("${bot.username}")
    private String username;

    @Value("${bot.payment-token}")
    private String providerToken;

    @Override
    public String getBotToken() {
        return token;
    }

    public String getProviderToken() {
        return providerToken;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Autowired
    private MessagesDAOService messagesDAOService;

    private SendInvoice buildInvoice(Integer chatId) {
        List<LabeledPrice> prices = new ArrayList<>();
        prices.add(new LabeledPrice("Coca Cola", 20000));

        SendInvoice invoice = new SendInvoice()
                .setProviderToken(getProviderToken())
                .setChatId(chatId)
                .setCurrency("RUB")
                .setDescription("Coca Cola")
                .setFlexible(false)
                .setPayload("cola-payload")
                .setPhotoUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/8/81/Coca-Cola_Glas_mit_Eis.jpg/800px-Coca-Cola_Glas_mit_Eis.jpg")
                .setTitle("Coca Cola")
                .setStartParameter("foo")
                .setNeedShippingAddress(false)
                .setPrices(prices);
        return invoice;
    }

    @Override
    public void onUpdateReceived(Update update) {

        Optional.ofNullable(update.getPreCheckoutQuery()).ifPresent(preCheckoutQuery -> {
            AnswerPreCheckoutQuery answer = new AnswerPreCheckoutQuery(preCheckoutQuery.getId(), true);
            try {
                sendApiMethod(answer);
            } catch (TelegramApiException e) {
                logger.error("Error: {}, cause: {}", e.getMessage(), e.getCause());
            }
        });

        if (!update.hasMessage())
            return;

        Message message = update.getMessage();

        Optional.ofNullable(message.getFrom()).ifPresent(user -> {
            TelegramMessage m = new TelegramMessage();
            m.setUserId(user.getId().toString());
            m.setText(message.getText());

            messagesDAOService.saveMassage(m);

        });

        Optional.ofNullable(message.getEntities())
                .orElse(Collections.emptyList()).stream()
                .filter(me -> StringUtils.equals(me.getText(), "/start"))
                .findFirst()
                .ifPresent(messageEntity -> {
                    SendMessage response = new SendMessage();
                    Long chatId = message.getChatId();
                    response.setChatId(chatId);
                    response.setText("Добрый день! Вы хотите что-нибудь купить?");
                    try {
                        sendApiMethod(response);
                    } catch (TelegramApiException e) {
                        logger.error("Error: {}, cause: {}", e.getMessage(), e.getCause());
                    }
                });

        Optional.ofNullable(message.getText()).filter(text -> text.contains("Что")).ifPresent(text -> {
            SendMessage response = new SendMessage();
            Long chatId = message.getChatId();
            response.setChatId(chatId);
            response.enableMarkdown(true);

            KeyboardRow row = new KeyboardRow();
            Stream.of("Coca Cola", "Nuclear Cola").forEach(mark -> row.add(mark));
            List<KeyboardRow> rows = new ArrayList<>();
            rows.add(row);

            response.setReplyMarkup(new ReplyKeyboardMarkup()
                    .setOneTimeKeyboard(true)
                    .setResizeKeyboard(true)
                    .setKeyboard(rows)
            );
            response.setText("Выбирайте!");

            try {
                sendApiMethod(response);
            } catch (TelegramApiException e) {
                logger.error("Error: {}, cause: {}", e.getMessage(), e.getCause());
            }
        });

        Optional.ofNullable(message.getText()).filter(text -> text.contains("Coca Cola")).ifPresent(text -> {
            Long chatId = message.getChatId();
            try {
                sendApiMethod(buildInvoice(chatId.intValue()));
            } catch (TelegramApiException e) {
                logger.error("Error: {}, cause: {}", e.getMessage(), e.getCause());
            }
        });

        Optional.ofNullable(message.getSuccessfulPayment()).ifPresent(payment -> {
            logger.info("{} {} just payed {} RUB", message.getFrom().getFirstName(), message.getFrom().getUserName(), payment.getTotalAmount() / 100);
        });

    }

}
