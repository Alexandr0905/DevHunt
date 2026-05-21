package com.devhunt.service.telegram;

import com.devhunt.model.SearchKeyword;
import com.devhunt.model.User;
import com.devhunt.model.Vacancy;
import com.devhunt.repository.UserRepository;
import com.devhunt.repository.VacancyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate; // ИМПОРТ РУЧНОЙ ТРАНЗАКЦИИ
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DevHuntBot extends TelegramLongPollingBot {

    private final UserRepository userRepository;
    private final VacancyRepository vacancyRepository;

    // Инструмент для принудительного открытия транзакции
    private final TransactionTemplate transactionTemplate;

    public static final Map<String, String> pendingTokens = new ConcurrentHashMap<>();
    private final String botName;

    public DevHuntBot(@Value("${telegram.bot.token}") String botToken,
                      @Value("${telegram.bot.name}") String botName,
                      UserRepository userRepository,
                      VacancyRepository vacancyRepository,
                      TransactionTemplate transactionTemplate) { // Добавили в конструктор
        super(botToken);
        this.botName = botName;
        this.userRepository = userRepository;
        this.vacancyRepository = vacancyRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // ЖЕСТКО ФОРСИРУЕМ ТРАНЗАКЦИЮ ВРУЧНУЮ
        transactionTemplate.executeWithoutResult(status -> {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();

                log.info("Получено сообщение от chatId {}: {}", chatId, messageText);

                try {
                    if (messageText.startsWith("/start")) {
                        handleStartCommand(chatId, messageText);
                    } else if (messageText.equals("/profile")) {
                        sendProfileInfo(chatId);
                    } else if (messageText.equals("/pause")) {
                        togglePause(chatId);
                    } else if (messageText.equals("/latest")) {
                        sendRandomVacancies(chatId);
                    } else {
                        sendMenu(chatId);
                    }
                } catch (Exception e) {
                    log.error("Критическая ошибка при обработке команды бота: ", e);
                    sendMessage(chatId, "⚠️ Произошла ошибка на сервере при обработке команды.");
                }
            }
        });
    }

    public void handleStartCommand(long chatId, String messageText) {
        String[] parts = messageText.split(" ");
        if (parts.length == 2) {
            String token = parts[1];
            String email = pendingTokens.get(token);

            if (email != null) {
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    user.setTelegramChatId(chatId);
                    user.setTelegramNotificationsEnabled(true);
                    userRepository.save(user);

                    pendingTokens.remove(token);
                    sendMessage(chatId, "✅ <b>Аккаунт успешно привязан!</b>\n\nБот активно отслеживает вакансии по вашим фильтрам.");
                    sendMenu(chatId);
                }
            } else {
                sendMessage(chatId, "❌ Ссылка устарела. Пожалуйста, сгенерируйте новую в профиле на сайте.");
            }
        } else {
            Optional<User> existingUser = userRepository.findByTelegramChatId(chatId);
            if (existingUser.isPresent()) {
                sendMessage(chatId, "✅ <b>Вы уже авторизованы!</b>");
                sendMenu(chatId);
            } else {
                sendMessage(chatId, "Добро пожаловать в DevHunt Bot! 🚀\n\nДля авторизации перейти по ссылке из своего профиля на сайте.");
            }
        }
    }

    public void sendMenu(long chatId) {
        String menu = "🤖 <b>Доступные команды:</b>\n\n" +
                "🔹 /latest — Получить 3 случайные вакансии по вашим фильтрам\n" +
                "🔹 /profile — Ваши текущие настройки фильтров\n" +
                "🔹 /pause — Включить/Выключить уведомления";
        sendMessage(chatId, menu);
    }

    public void sendProfileInfo(long chatId) {
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String status = user.isTelegramNotificationsEnabled() ? "Включены 🔔" : "На паузе 🔕";

            String keywords = user.getSubscribedKeywords().isEmpty() ?
                    "Любые (Вы не выбрали технологии на сайте)" :
                    user.getSubscribedKeywords().stream().map(SearchKeyword::getKeyword).collect(Collectors.joining(", "));

            String grades = user.getSubscribedGrades().isEmpty() ?
                    "Любые" :
                    user.getSubscribedGrades().stream().map(Enum::name).collect(Collectors.joining(", "));

            String text = String.format("👤 <b>Ваш профиль DevHunt</b>\n\n" +
                            "📧 <b>Аккаунт:</b> %s\n" +
                            "⚙️ <b>Статус:</b> %s\n" +
                            "🛠 <b>Технологии:</b> %s\n" +
                            "📈 <b>Грейды:</b> %s\n\n" +
                            "<i>Изменить фильтры можно на сайте!</i>",
                    user.getEmail(), status, keywords, grades);

            sendMessage(chatId, text);
        } else {
            sendMessage(chatId, "⚠️ Ваш аккаунт не привязан к сайту. Сгенерируйте ссылку в профиле.");
        }
    }

    public void togglePause(long chatId) {
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setTelegramNotificationsEnabled(!user.isTelegramNotificationsEnabled());
            userRepository.save(user);

            String status = user.isTelegramNotificationsEnabled() ?
                    "🔔 Уведомления <b>ВКЛЮЧЕНЫ</b>. Я снова слежу за рынком!" :
                    "🔕 Уведомления <b>ПРИОСТАНОВЛЕНЫ</b>. Не буду вас беспокоить.";
            sendMessage(chatId, status);
        } else {
            sendMessage(chatId, "⚠️ Аккаунт не привязан.");
        }
    }

    public void sendRandomVacancies(long chatId) {
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "⚠️ Ваш аккаунт не привязан к сайту.");
            return;
        }

        User user = userOpt.get();
        List<Vacancy> allVacancies = vacancyRepository.findAll();

        List<Vacancy> matched = allVacancies.stream()
                .filter(Vacancy::getActiveStatus)
                .filter(v -> {
                    boolean gradeMatch = user.getSubscribedGrades().isEmpty() || user.getSubscribedGrades().contains(v.getGrade());
                    if (!gradeMatch) return false;

                    if (user.getSubscribedKeywords().isEmpty()) return true;

                    String lowerTitle = v.getTitle().toLowerCase();
                    return user.getSubscribedKeywords().stream()
                            .anyMatch(kw -> lowerTitle.contains(kw.getKeyword().toLowerCase()));
                })
                .collect(Collectors.toList());

        if (matched.isEmpty()) {
            sendMessage(chatId, "К сожалению, прямо сейчас в базе нет активных вакансий по вашим фильтрам (проверьте, выбраны ли технологии на сайте) 😔");
            return;
        }

        Collections.shuffle(matched);
        List<Vacancy> top3 = matched.stream().limit(3).toList();

        sendMessage(chatId, "🎲 <b>Вот 3 случайные вакансии специально для вас:</b>");

        for (Vacancy v : top3) {
            String salary = v.getSalaryUsd() != null ?
                    String.format("%,.0f ₽", v.getSalaryUsd().doubleValue() * 90) :
                    (v.getSalaryOrig() != null ? v.getSalaryOrig() : "Не указана");

            String msg = String.format(
                    "💼 <b>%s</b>\n" +
                            "🏢 <b>Компания:</b> %s\n" +
                            "📈 <b>Грейд:</b> %s\n" +
                            "💰 <b>Зарплата:</b> %s\n\n" +
                            "<a href=\"%s\">👉 Открыть на %s</a>",
                    v.getTitle(), v.getCompany(), v.getGrade() != null ? v.getGrade().name() : "Любой", salary, v.getUrl(), v.getSource()
            );
            sendMessage(chatId, msg);
        }
    }

    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");
        message.setDisableWebPagePreview(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке в ТГ: {}", e.getMessage());
        }
    }
}