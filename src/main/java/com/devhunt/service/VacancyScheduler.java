package com.devhunt.service;

import com.devhunt.model.Vacancy;
import com.devhunt.repository.VacancyRepository;
import com.devhunt.service.scraper.JobScraper;
import com.devhunt.service.scraper.ScraperFactory;
import com.devhunt.service.telegram.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VacancyScheduler {

    private final ScraperFactory scraperFactory;
    private final VacancyRepository vacancyRepository;

    // 1. Внедряем наш сервис уведомлений
    private final TelegramNotificationService telegramNotificationService;

    // Отключаем автоматический запуск каждую минуту, чтобы не ловить баны
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void runScrapers() {
        log.info("--- STARTING SCRAPING SESSION ---");

        List<JobScraper> scrapers = scraperFactory.getAllScrapers();

        for (JobScraper scraper : scrapers) {
            try {
                String source = scraper.getSourceName();
                log.info("Running scraper: {}", source);

                int deactivatedCount = vacancyRepository.markAllInactiveBySource(source);
                log.info("Source {}: marked {} vacancies as inactive", source, deactivatedCount);

                List<Vacancy> newVacancies = scraper.scrapeJobs();
                log.info("Source {}: scraper found {} raw vacancies", source, newVacancies.size());

                int savedCount = 0;

                for (Vacancy v : newVacancies) {
                    try {
                        vacancyRepository.findByUrl(v.getUrl()).ifPresentOrElse(
                                existing -> {
                                    // Вакансия уже есть, просто обновляем данные (пуш НЕ шлем)
                                    existing.setActiveStatus(true);
                                    existing.setTitle(v.getTitle());
                                    existing.setCompany(v.getCompany());
                                    existing.setSalaryOrig(v.getSalaryOrig());
                                    existing.setSalaryUsd(v.getSalaryUsd());
                                    existing.setGrade(v.getGrade());
                                    existing.setRemote(v.getRemote());
                                    existing.setDeletedAt(null);
                                    vacancyRepository.save(existing);
                                },
                                () -> {
                                    // ЭТО НОВАЯ ВАКАНСИЯ!
                                    Vacancy saved = vacancyRepository.save(v);

                                    // 2. Вытаскиваем тег из названия, чтобы бот понял, кому это слать
                                    String guessedKeyword = extractKeywordFromTitle(saved.getTitle());

                                    // 3. Дергаем Телеграм-рассылку
                                    telegramNotificationService.notifyAboutNewVacancy(saved, guessedKeyword);
                                }
                        );
                        savedCount++;
                    } catch (Exception e) {
                        log.warn("Failed to save vacancy from {}: {}", source, v.getUrl());
                    }
                }
                log.info("Source {}: Successfully processed {} vacancies.", source, savedCount);

            } catch (Exception e) {
                log.error("CRITICAL ERROR in scraper {}: ", scraper.getSourceName(), e);
            }
        }
        log.info("--- SCRAPING SESSION COMPLETED ---");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application started. Initializing first-time scraping...");
        runScrapers();
    }

    // Простой хелпер для определения технологии по тексту заголовка
    private String extractKeywordFromTitle(String title) {
        if (title == null) return "Other";
        String lowerTitle = title.toLowerCase();

        if (lowerTitle.contains("java") && !lowerTitle.contains("javascript")) return "Java";
        if (lowerTitle.contains("react")) return "React";
        if (lowerTitle.contains("qa") || lowerTitle.contains("тестировщик")) return "QA";
        if (lowerTitle.contains("python")) return "Python";
        if (lowerTitle.contains("go") || lowerTitle.contains("golang")) return "Go";
        if (lowerTitle.contains("ruby")) return "Ruby";
        if (lowerTitle.contains("php")) return "PHP";
        if (lowerTitle.contains("spring")) return "Spring";
        if (lowerTitle.contains("frontend") || lowerTitle.contains("фронтенд")) return "Frontend";
        if (lowerTitle.contains("backend") || lowerTitle.contains("бэкенд")) return "Backend";

        return "IT"; // Дефолт, если ничего не подошло
    }
}