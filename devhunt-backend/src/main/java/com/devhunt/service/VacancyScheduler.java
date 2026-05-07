package com.devhunt.service;

import com.devhunt.model.Vacancy;
import com.devhunt.repository.VacancyRepository;
import com.devhunt.service.scraper.JobScraper;
import com.devhunt.service.scraper.ScraperFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // 1. Отключаем автоматический запуск каждую минуту, чтобы не ловить баны
    // @Scheduled(cron = "0 * * * * *")
    @Transactional // Добавляем общую транзакцию на процесс
    public void runScrapers() {
        log.info("--- STARTING SCRAPING SESSION ---");

        List<JobScraper> scrapers = scraperFactory.getAllScrapers();

        for (JobScraper scraper : scrapers) {
            try {
                String source = scraper.getSourceName();
                log.info("Running scraper: {}", source);

                // 2. Деактивируем старые вакансии этого источника
                int deactivatedCount = vacancyRepository.markAllInactiveBySource(source);
                log.info("Source {}: marked {} vacancies as inactive", source, deactivatedCount);

                List<Vacancy> newVacancies = scraper.scrapeJobs();
                log.info("Source {}: scraper found {} raw vacancies", source, newVacancies.size());

                int savedCount = 0;
                int updatedCount = 0;

                // 3. СОХРАНЕНИЕ / ОБНОВЛЕНИЕ
                for (Vacancy v : newVacancies) {
                    try {
                        vacancyRepository.findByUrl(v.getUrl()).ifPresentOrElse(
                                existing -> {
                                    existing.setActiveStatus(true);
                                    existing.setTitle(v.getTitle());
                                    existing.setCompany(v.getCompany()); // Не забываем компанию
                                    existing.setSalaryOrig(v.getSalaryOrig());
                                    existing.setSalaryUsd(v.getSalaryUsd());
                                    existing.setGrade(v.getGrade());     // Обновляем грейд
                                    existing.setRemote(v.getRemote());   // И удаленку
                                    existing.setDeletedAt(null);
                                    vacancyRepository.save(existing);
                                },
                                () -> {
                                    vacancyRepository.save(v);
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
}