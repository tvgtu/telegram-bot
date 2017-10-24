#!/bin/sh -x
set -e

exec java -jar -Dbot.token=${TELEGRAM_BOT_TOKEN} -Dbot.username=${TELEGRAM_BOT_USERNAME} telegram-bot.jar