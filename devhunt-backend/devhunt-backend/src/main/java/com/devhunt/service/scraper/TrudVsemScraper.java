package com.devhunt.service.scraper;

import com.devhunt.model.Vacancy;
import com.devhunt.model.enums.Grade;
import com.devhunt.service.CurrencyService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrudVsemScraper implements JobScraper {

    private final CurrencyService currencyService;
    private final RestClient restClient = RestClient.create();

    @Override
    public String getSourceName() {
        return "TrudVsem (Работа России)";
    }

    @Override
    public List<Vacancy> scrapeJobs() {
        List<Vacancy> allVacancies = new ArrayList<>();
        String[] searchQueries = {"Java", "Python", "Frontend", "Аналитик", "DevOps"};

        log.info("Starting mass scraping from TrudVsem...");

        for (String query : searchQueries) {
            try {
                // Официальное, открытое государственное API
                String url = "https://opendata.trudvsem.ru/api/v1/vacancies?text=" + query + "&limit=50";

                JsonNode root = restClient.get()
                        .uri(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .retrieve()
                        .body(JsonNode.class);

                JsonNode results = root.path("results").path("vacancies");

                if (results.isArray()) {
                    for (JsonNode itemWrapper : results) {
                        try {
                            JsonNode item = itemWrapper.path("vacancy");

                            String title = item.path("job-name").asText();
                            String company = item.path("company").path("name").asText();
                            String vacancyUrl = item.path("vac_url").asText();

                            // Формируем вилку зарплаты
                            String salaryOrig = "Зарплата не указана";
                            int min = item.path("salary_min").asInt(0);
                            int max = item.path("salary_max").asInt(0);

                            if (min > 0 && max > 0) {
                                if (min == max) {
                                    salaryOrig = String.format("%d руб.", min); // Точная сумма
                                } else {
                                    salaryOrig = String.format("от %d до %d руб.", min, max); // Вилка
                                }
                            } else if (min > 0) {
                                salaryOrig = String.format("от %d руб.", min);
                            } else if (max > 0) {
                                salaryOrig = String.format("до %d руб.", max);
                            }

                            BigDecimal salaryUsd = null;
                            if (min > 0) {
                                salaryUsd = currencyService.convertToUsd(BigDecimal.valueOf(min), "RUB");
                            }

                            // Описание: склеиваем обязанности и требования
                            String duty = item.path("duty").asText("");
                            String req = item.path("requirement").path("qualification").asText("");
                            String description = (duty + "\n\nТребования:\n" + req).trim().replaceAll("<[^>]*>", "");

                            String fullText = (title + " " + description).toLowerCase();
                            Grade grade = Grade.UNKNOWN;
                            if (fullText.contains("junior") || fullText.contains("младший") || fullText.contains("без опыта")) grade = Grade.JUNIOR;
                            else if (fullText.contains("middle") || fullText.contains("средний")) grade = Grade.MIDDLE;
                            else if (fullText.contains("senior") || fullText.contains("старший") || fullText.contains("ведущий")) grade = Grade.SENIOR;
                            else if (fullText.contains("lead") || fullText.contains("руководитель")) grade = Grade.LEAD;

                            boolean remote = fullText.contains("удален");

                            allVacancies.add(Vacancy.builder()
                                    .title(title)
                                    .company(company)
                                    .url(vacancyUrl)
                                    .source(getSourceName())
                                    .salaryOrig(salaryOrig)
                                    .salaryUsd(salaryUsd)
                                    .description(description)
                                    .grade(grade)
                                    .remote(remote)
                                    .activeStatus(true)
                                    .build());

                        } catch (Exception e) {
                            log.warn("Failed to parse TrudVsem item");
                        }
                    }
                }
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("TrudVsem API failed for {}: {}", query, e.getMessage());
            }
        }
        log.info("TrudVsem scraping finished. Gathered: {} vacancies.", allVacancies.size());
        return allVacancies;
    }
}