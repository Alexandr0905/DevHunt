package com.devhunt.service.telegram;

import com.devhunt.model.User;
import com.devhunt.model.Vacancy;
import com.devhunt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationService {

    private final UserRepository userRepository;
    private final DevHuntBot devHuntBot;

    public void notifyAboutNewVacancy(Vacancy vacancy, String keywordName) {
        // Достаем всех юзеров (в реальном хайлоаде тут был бы хитрый SQL, но для MVP ок)
        List<User> users = userRepository.findAll();

        for (User user : users) {
            // Пропускаем тех, у кого не привязан ТГ или выключен тумблер
            if (user.getTelegramChatId() == null || !user.isTelegramNotificationsEnabled()) {
                continue;
            }

            // Проверяем фильтр по ключевым словам (если чипсы не выбраны вообще - шлем всё)
            boolean keywordMatch = user.getSubscribedKeywords().isEmpty() ||
                    user.getSubscribedKeywords().stream()
                            .anyMatch(k -> k.getKeyword().equalsIgnoreCase(keywordName));

            // Проверяем фильтр по грейдам
            boolean gradeMatch = user.getSubscribedGrades().isEmpty() ||
                    user.getSubscribedGrades().contains(vacancy.getGrade());

            if (keywordMatch && gradeMatch) {
                sendPush(user.getTelegramChatId(), vacancy, keywordName);
            }
        }
    }

    private void sendPush(Long chatId, Vacancy vacancy, String keywordName) {
        String salary = vacancy.getSalaryUsd() != null ?
                String.format("%,.0f ₽", vacancy.getSalaryUsd().doubleValue() * 90) :
                (vacancy.getSalaryOrig() != null ? vacancy.getSalaryOrig() : "Не указана");

        String message = String.format(
                "🔥 <b>Новая вакансия: %s</b>\n\n" +
                        "💼 <b>Позиция:</b> %s\n" +
                        "🏢 <b>Компания:</b> %s\n" +
                        "📈 <b>Грейд:</b> %s\n" +
                        "💰 <b>Зарплата:</b> %s\n\n" +
                        "<a href=\"%s\">👉 Открыть вакансию на %s</a>",
                keywordName,
                vacancy.getTitle(),
                vacancy.getCompany(),
                vacancy.getGrade() != null ? vacancy.getGrade().name() : "Любой",
                salary,
                vacancy.getUrl(),
                vacancy.getSource()
        );

        devHuntBot.sendMessage(chatId, message);
        log.info("Отправлен пуш в ТГ пользователю {} по вакансии {}", chatId, vacancy.getTitle());
    }
}