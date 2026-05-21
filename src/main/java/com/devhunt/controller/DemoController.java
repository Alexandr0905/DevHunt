package com.devhunt.controller;

import com.devhunt.model.SearchKeyword;
import com.devhunt.model.User;
import com.devhunt.model.Vacancy;
import com.devhunt.model.enums.Grade;
import com.devhunt.repository.UserRepository;
import com.devhunt.repository.VacancyRepository;
import com.devhunt.service.telegram.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final TelegramNotificationService notificationService;
    private final UserRepository userRepository;
    private final VacancyRepository vacancyRepository;

    @PostMapping("/simulate-push")
    public ResponseEntity<?> simulatePush(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Подстраиваем тестовую вакансию под профиль текущего юзера
        String testKeyword = "Java";
        Optional<SearchKeyword> firstKw = user.getSubscribedKeywords().stream().findFirst();
        if (firstKw.isPresent()) {
            testKeyword = firstKw.get().getKeyword();
        }

        Grade testGrade = Grade.MIDDLE;
        Optional<Grade> firstGrade = user.getSubscribedGrades().stream().findFirst();
        if (firstGrade.isPresent()) {
            testGrade = firstGrade.get();
        }

        // Генерируем "жирную" фейковую вакансию
        Vacancy fakeVacancy = Vacancy.builder()
                .title("Senior/Lead " + testKeyword + " Developer (Remote)")
                .company("Yandex AI / DevHunt Secret Lab")
                .url("https://hh.ru/vacancy/" + System.currentTimeMillis())
                .source("HH.ru")
                .grade(testGrade)
                .remote(true)
                .salaryOrig("от 400 000 ₽ на руки")
                .salaryUsd(new BigDecimal("4500.00"))
                .description("Тестовая вакансия для демонстрации пуш-уведомлений.")
                .activeStatus(true)
                .build();

        // Сохраняем в базу, чтобы всё было по-настоящему
        vacancyRepository.save(fakeVacancy);

        // Триггерим отправку в ТГ
        notificationService.notifyAboutNewVacancy(fakeVacancy, testKeyword);

        return ResponseEntity.ok(Map.of("message", "Демо-пуш успешно отправлен в Telegram!"));
    }
}