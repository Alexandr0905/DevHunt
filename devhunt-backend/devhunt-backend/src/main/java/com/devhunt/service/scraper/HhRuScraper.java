package com.devhunt.service.scraper;

import com.devhunt.model.Vacancy;
import com.devhunt.model.enums.Grade;
import com.devhunt.service.CurrencyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
//@Component
@RequiredArgsConstructor
public class HhRuScraper implements JobScraper {

    private final CurrencyService currencyService;
    private final ObjectMapper objectMapper;

    @Override
    public String getSourceName() {
        return "HH.ru";
    }

    @Override
    public List<Vacancy> scrapeJobs() {
        List<Vacancy> vacancies = new ArrayList<>();
        log.info("Starting HH.ru API scraping using Playwright Chrome Bypass...");

        try (Playwright playwright = Playwright.create()) {
            // Запускаем реальный движок Chrome
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            // ШАГ 1: Заходим на главную, чтобы пройти проверку Qrator и получить куки реального человека
            log.info("Bypassing Qrator protection...");
            page.navigate("https://hh.ru");
            page.waitForLoadState();

            // ШАГ 2: Выполняем JS-fetch прямо из консоли страницы. Для защиты это выглядит как обычный запрос из браузера.
            log.info("Fetching API data from inside the browser context...");
            Object response = page.evaluate("async () => {" +
                    "  const res = await fetch('https://api.hh.ru/vacancies?text=Java&per_page=50&area=113');" +
                    "  return await res.text();" +
                    "}");

            String jsonResponse = (String) response;
            JsonNode root = objectMapper.readTree(jsonResponse);

            if (root.has("items")) {
                JsonNode items = root.get("items");
                log.info("SUCCESS! Bypassed protection. Found 'items' array. Size: {}", items.size());

                for (JsonNode item : items) {
                    Vacancy vacancy = parseNodeToVacancy(item);
                    if (vacancy != null) {
                        vacancies.add(vacancy);
                    }
                }
            } else {
                log.error("Failed to find items. Response: {}", jsonResponse);
            }

            browser.close();
        } catch (Exception e) {
            log.error("CRITICAL: Playwright bypass failed. Reason: ", e);
        }

        log.info("Finished HH.ru scraping. Found {} vacancies.", vacancies.size());
        return vacancies;
    }

    private Vacancy parseNodeToVacancy(JsonNode item) {
        try {
            String title = item.path("name").asText();
            String company = item.path("employer").path("name").asText();
            String url = item.path("alternate_url").asText();
            String city = item.path("area").path("name").asText();

            Grade grade = Grade.UNKNOWN;
            String exp = item.path("experience").path("id").asText();
            if ("noExperience".equals(exp)) grade = Grade.INTERN;
            else if (title.toLowerCase().contains("junior")) grade = Grade.JUNIOR;
            else if (title.toLowerCase().contains("middle")) grade = Grade.MIDDLE;
            else if (title.toLowerCase().contains("senior")) grade = Grade.SENIOR;
            else if (title.toLowerCase().contains("lead")) grade = Grade.LEAD;

            String salaryOrig = null;
            BigDecimal salaryUsd = null;
            JsonNode salaryNode = item.path("salary");

            if (!salaryNode.isMissingNode() && !salaryNode.isNull()) {
                String currency = salaryNode.path("currency").asText("RUB").replace("RUR", "RUB");
                Integer from = salaryNode.path("from").isNull() ? null : salaryNode.path("from").asInt();
                Integer to = salaryNode.path("to").isNull() ? null : salaryNode.path("to").asInt();

                if (from != null && to != null) {
                    salaryOrig = String.format("%d - %d %s", from, to, currency);
                    salaryUsd = currencyService.convertToUsd(BigDecimal.valueOf((from + to) / 2.0), currency);
                } else if (from != null) {
                    salaryOrig = String.format("от %d %s", from, currency);
                    salaryUsd = currencyService.convertToUsd(BigDecimal.valueOf(from), currency);
                } else if (to != null) {
                    salaryOrig = String.format("до %d %s", to, currency);
                    salaryUsd = currencyService.convertToUsd(BigDecimal.valueOf(to), currency);
                }
            }

            boolean remote = "remote".equals(item.path("schedule").path("id").asText());

            String rawDescription = item.path("snippet").path("requirement").asText("") + "\n" + item.path("snippet").path("responsibility").asText("");
            String cleanDescription = rawDescription.replaceAll("<[^>]*>", "").replace("null", "").trim();

            return Vacancy.builder()
                    .title(title)
                    .company(company)
                    .url(url)
                    .source(getSourceName())
                    .city(city)
                    .grade(grade)
                    .remote(remote)
                    .salaryOrig(salaryOrig)
                    .salaryUsd(salaryUsd)
                    .description(cleanDescription)
                    .activeStatus(true)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse HH vacancy node: {}", e.getMessage());
            return null;
        }
    }
}