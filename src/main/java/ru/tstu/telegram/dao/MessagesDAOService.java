package ru.tstu.telegram.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;
import ru.tstu.telegram.model.TelegramMessage;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * Created by user on 15.11.17.
 */
@Repository
public class MessagesDAOService extends JdbcDaoSupport {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    private void initialize() {
        setDataSource(dataSource);
    }

    public void saveMassage(TelegramMessage message) {
        getJdbcTemplate().update("INSERT INTO MESSAGES (USER_ID, MESSAGE) VALUES (?, ?)",
                message.getUserId(), message.getText());
    }
}
