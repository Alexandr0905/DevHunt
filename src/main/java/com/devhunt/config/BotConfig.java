package com.devhunt.config;

import com.devhunt.service.telegram.DevHuntBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(DevHuntBot devHuntBot) throws TelegramApiException {
        // Создаем сессию для подключения к серверам Телеграма
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        // Принудительно регистрируем нашего бота
        api.registerBot(devHuntBot);
        return api;
    }
}