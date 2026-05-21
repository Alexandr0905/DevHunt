package com.devhunt.service.scraper;

import com.devhunt.model.SearchKeyword;
import com.devhunt.model.Vacancy;
import com.devhunt.model.enums.Grade;
import com.devhunt.repository.SearchKeywordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeekjobScraper implements JobScraper {

    private final SearchKeywordRepository keywordRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String getSourceName() {
        return "Geekjob";
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

        log.info("Starting Geekjob DOM scraping using Playwright...");

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            for (String query : searchQueries) {
                try {
                    String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                    String url = "https://geekjob.ru/vacancies?qs=" + encodedQuery;

                    log.info("Loading Geekjob page for: {}", query);
                    page.navigate(url);
                    page.waitForLoadState();

                    // Выполняем JS скрипт, который собирает массив карточек прям со страницы
                    String extractScript = """
                        () => {
                            let results = [];
                            // Ищем все карточки вакансий на странице
                            let cards = document.querySelectorAll('a.title'); 
                            cards.forEach(card => {
                                let title = card.innerText || '';
                                let link = card.href || '';
                                
                                // Поднимаемся к родителю, чтобы вытащить компанию
                                let container = card.closest('div.row, li, div.collection-item');
                                let company = '';
                                if (container) {
                                    let compNode = container.querySelector('.company-name');
                                    if (compNode) company = compNode.innerText;
                                }
                                
                                results.push({ title: title, url: link, company: company });
                            });
                            return JSON.stringify(results);
                        }
                    """;

                    Object response = page.evaluate(extractScript);
                    String json = (String) response;

                    // Парсим собранный JSON обратно в Java объекты
                    List<Map<String, String>> parsedItems = objectMapper.readValue(json, new TypeReference<>() {});

                    for (Map<String, String> item : parsedItems) {
                        String title = item.get("title");
                        String link = item.get("url");
                        String company = item.get("company");

                        if (title == null || title.isBlank() || link == null) continue;

                        String fullText = title.toLowerCase();
                        Grade grade = Grade.UNKNOWN;
                        if (fullText.contains("junior")) grade = Grade.JUNIOR;
                        else if (fullText.contains("middle")) grade = Grade.MIDDLE;
                        else if (fullText.contains("senior")) grade = Grade.SENIOR;
                        else if (fullText.contains("lead")) grade = Grade.LEAD;

                        vacancies.add(Vacancy.builder()
                                .title(title)
                                .company(company != null && !company.isBlank() ? company : "Не указана")
                                .url(link)
                                .source(getSourceName())
                                .grade(grade)
                                .remote(fullText.contains("remote") || fullText.contains("удален"))
                                .activeStatus(true)
                                .description("Откройте ссылку, чтобы посмотреть полное описание") // На странице поиска описания обычно нет
                                .build());
                    }

                    Thread.sleep(2000); // Чтобы Geekjob не откинул капчу
                } catch (Exception e) {
                    log.error("Failed to scrape Geekjob for query: {}", query, e);
                }
            }
            browser.close();
        } catch (Exception e) {
            log.error("CRITICAL: Playwright failed on Geekjob. Reason: ", e);
        }

        log.info("Finished Geekjob scraping. Found {} vacancies.", vacancies.size());
        return vacancies;
    }
}