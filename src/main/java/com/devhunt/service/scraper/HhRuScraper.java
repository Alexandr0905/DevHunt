package com.devhunt.service.scraper;

import com.devhunt.model.SearchKeyword;
import com.devhunt.model.Vacancy;
import com.devhunt.model.enums.Grade;
import com.devhunt.repository.SearchKeywordRepository;
import com.devhunt.service.CurrencyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class HhRuScraper implements JobScraper {

    private final CurrencyService currencyService;
    private final SearchKeywordRepository keywordRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String getSourceName() {
        return "HH.ru";
    }

    @Override
    public List<Vacancy> scrapeJobs() {
        List<Vacancy> vacancies = new ArrayList<>();

        List<String> searchQueries = keywordRepository.findAll().stream()
                .map(SearchKeyword::getKeyword)
                .collect(Collectors.toList());

        if (searchQueries.isEmpty()) {
            searchQueries.add("Java");
        }

        log.info("Starting HH.ru HTML DOM scraping in HEADFUL mode...");

        try (Playwright playwright = Playwright.create()) {
            // ВРУБАЕМ ВИЗУАЛЬНЫЙ БРАУЗЕР И УБИВАЕМ ФЛАГИ БОТА
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(false) // <-- ТЕПЕРЬ ТЫ УВИДИШЬ БРАУЗЕР СВОИМИ ГЛАЗАМИ
                    .setArgs(List.of("--disable-blink-features=AutomationControlled")));

            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .setViewportSize(1280, 800);

            Page page = browser.newContext(contextOptions).newPage();

            for (String query : searchQueries) {
                try {
                    String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                    String url = "https://hh.ru/search/vacancy?text=" + encodedQuery + "&area=113&items_on_page=50";

                    log.info("Loading HH.ru page for: {}", query);
                    page.navigate(url);
                    page.waitForLoadState();

                    // ЖДЕМ КАРТОЧКИ 5 СЕКУНД. Если их нет - вылезла капча
                    try {
                        page.waitForSelector("[data-qa='vacancy-serp__vacancy'], .serp-item, [data-qa='vacancy-card']", new Page.WaitForSelectorOptions().setTimeout(5000));
                    } catch (Exception e) {
                        log.warn("⚠️ АЛАРМ: Карточки не найдены для '{}'. Скорее всего, вылезла КАПЧА! Кликни её руками в открытом окне хрома.", query);
                        // Даем тебе 15 секунд на то чтобы прожать капчу руками
                        Thread.sleep(15000);
                    }

                    // Сверх-живучий JS-скрипт с десятком разных классов (HH любит их менять)
                    String extractScript = """
                        () => {
                            let results = [];
                            let cards = document.querySelectorAll('[data-qa="vacancy-serp__vacancy"], .serp-item, [data-qa="vacancy-card"]');
                            cards.forEach(c => {
                                let titleNode = c.querySelector('[data-qa="vacancy-serp__vacancy-title"], [data-qa="serp-item__title"], [data-qa="vacancy-title"], h2 a');
                                let empNode = c.querySelector('[data-qa="vacancy-serp__vacancy-employer"], [data-qa="vacancy-card-employer-title"]');
                                let salNode = c.querySelector('[data-qa="vacancy-serp__vacancy-compensation"], [data-qa="vacancy-card-compensation"]');
                                
                                if (titleNode && titleNode.innerText) {
                                    results.push({
                                        title: titleNode.innerText || '',
                                        url: titleNode.href || '',
                                        company: empNode ? empNode.innerText : '',
                                        salary: salNode ? salNode.innerText : ''
                                    });
                                }
                            });
                            return JSON.stringify(results);
                        }
                    """;

                    Object response = page.evaluate(extractScript);
                    String json = (String) response;

                    List<Map<String, String>> parsedItems = objectMapper.readValue(json, new TypeReference<>() {});

                    if (parsedItems.isEmpty()) {
                        log.warn("Скрипт не нашел данные в DOM-дереве для '{}'.", query);
                    }

                    for (Map<String, String> item : parsedItems) {
                        Vacancy vacancy = parseMapToVacancy(item);
                        if (vacancy != null) {
                            vacancies.add(vacancy);
                        }
                    }

                    // Задержка перед следующим словом, чтобы не злить защиту
                    Thread.sleep(2500);
                } catch (Exception e) {
                    log.error("Failed to scrape HH DOM for query: {}", query, e);
                }
            }
            browser.close();
        } catch (Exception e) {
            log.error("CRITICAL: Playwright failed on HH.ru. Reason: ", e);
        }

        log.info("Finished HH.ru HTML scraping. Found {} vacancies.", vacancies.size());
        return vacancies;
    }

    private Vacancy parseMapToVacancy(Map<String, String> item) {
        try {
            String title = item.get("title");
            String url = item.get("url");
            String company = item.get("company");
            String salaryOrig = item.get("salary");

            if (title == null || title.isBlank() || url == null) return null;

            String fullText = title.toLowerCase();
            Grade grade = Grade.UNKNOWN;
            if (fullText.contains("стажер") || fullText.contains("intern")) grade = Grade.INTERN;
            else if (fullText.contains("junior") || fullText.contains("младший")) grade = Grade.JUNIOR;
            else if (fullText.contains("middle")) grade = Grade.MIDDLE;
            else if (fullText.contains("senior") || fullText.contains("старший")) grade = Grade.SENIOR;
            else if (fullText.contains("lead") || fullText.contains("ведущий")) grade = Grade.LEAD;

            BigDecimal salaryUsd = null;
            if (salaryOrig != null && !salaryOrig.isBlank()) {
                // Вычищаем неразрывные пробелы из формата "от 150 000 ₽"
                salaryOrig = salaryOrig.replace(" ", "").replace(" ", "").replace(" ", "");
                Pattern p = Pattern.compile("\\d+");
                Matcher m = p.matcher(salaryOrig);
                if (m.find()) {
                    BigDecimal val = new BigDecimal(m.group());
                    if (salaryOrig.contains("$") || salaryOrig.toLowerCase().contains("usd")) {
                        salaryUsd = val;
                    } else {
                        salaryUsd = currencyService.convertToUsd(val, "RUB");
                    }
                }
            } else {
                salaryOrig = "Зарплата не указана";
            }

            return Vacancy.builder()
                    .title(title)
                    .company(company != null && !company.isBlank() ? company.replace(" ", " ") : "Не указана")
                    .url(url)
                    .source(getSourceName())
                    .grade(grade)
                    .remote(fullText.contains("удален") || fullText.contains("remote"))
                    .salaryOrig(salaryOrig)
                    .salaryUsd(salaryUsd)
                    .description("Откройте ссылку, чтобы посмотреть полное описание")
                    .activeStatus(true)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse HH vacancy map: {}", e.getMessage());
            return null;
        }
    }
}