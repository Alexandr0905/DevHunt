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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class HabrCareerScraper implements JobScraper {

    private final CurrencyService currencyService;
    private final RestClient restClient = RestClient.create();

    @Override
    public String getSourceName() {
        return "Habr Career";
    }

    @Override
    public List<Vacancy> scrapeJobs() {
        List<Vacancy> allVacancies = new ArrayList<>();
        String[] searchQueries = {"Java", "Python", "Frontend", "React", "DevOps", "QA", "Data Science", "Mobile"};

        log.info("Starting mass scraping from Habr Career API (with FULL descriptions)...");

        for (String query : searchQueries) {
            try {
                String url = "https://career.habr.com/api/frontend/vacancies?q=" + query + "&sort=relevance&type=all&per_page=50";

                JsonNode root = restClient.get()
                        .uri(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .retrieve()
                        .body(JsonNode.class);

                if (root != null && root.has("list")) {
                    JsonNode items = root.get("list");

                    // Превращаем JsonNode в обычный список для стрима
                    List<JsonNode> itemList = new ArrayList<>();
                    items.forEach(itemList::add);

                    // ПАРАЛЛЕЛЬНЫЙ ПАРСИНГ (Ускорит процесс в 5-10 раз)
                    itemList.parallelStream().forEach(item -> {
                        try {
                            String title = item.path("title").asText();
                            String company = item.path("company").path("title").asText();
                            String vacancyUrl = "https://career.habr.com" + item.path("href").asText();

                            String salaryOrig = null;
                            JsonNode salaryNode = item.path("salary");
                            if (!salaryNode.isMissingNode() && !salaryNode.isNull()) {
                                salaryOrig = salaryNode.has("formatted") ? salaryNode.path("formatted").asText() : salaryNode.asText();
                            }
                            if (salaryOrig == null || salaryOrig.isBlank() || salaryOrig.equals("null")) {
                                salaryOrig = "Зарплата не указана";
                            }
                            BigDecimal salaryUsd = parseSalaryToUsd(salaryOrig);

                            // Метод fetchFullDescription остался тем же, но теперь они вызываются параллельно
                            String fullDescription = fetchFullDescription(vacancyUrl);

                            String fullTextContext = (title + " " + item.toString() + " " + fullDescription).toLowerCase();
                            Grade grade = detectGrade(fullTextContext);
                            boolean remote = fullTextContext.contains("удаленка") || fullTextContext.contains("remote") || item.path("remote").asBoolean();

                            Vacancy vacancy = Vacancy.builder()
                                    .title(title)
                                    .company(company)
                                    .url(vacancyUrl)
                                    .source(getSourceName())
                                    .salaryOrig(salaryOrig)
                                    .salaryUsd(salaryUsd)
                                    .description(fullDescription)
                                    .grade(grade)
                                    .remote(remote)
                                    .activeStatus(true)
                                    .build();

                            // Безопасное добавление в общий список из разных потоков
                            synchronized (allVacancies) {
                                allVacancies.add(vacancy);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse individual Habr item", e);
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Habr API failed for query {}: {}", query, e.getMessage());
            }
        }

        log.info("Mass scraping finished. Total gathered: {} vacancies.", allVacancies.size());
        return allVacancies;
    }

    // Метод, который вытаскивает текст "Чем предстоит заниматься" прямо из HTML
    private String fetchFullDescription(String url) {
        try {
            // ВАЖНО: Задержка 150мс, чтобы Хабр не заблокировал IP за DDoS (300+ запросов подряд)
            //Thread.sleep(150);

            String html = restClient.get()
                    .uri(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .retrieve()
                    .body(String.class);

            if (html == null) return "Описание не найдено.";

            // Ищем блок описания в HTML
            int startIndex = html.indexOf("vacancy-description__text");
            if (startIndex == -1) startIndex = html.indexOf("style-ugc");

            if (startIndex != -1) {
                startIndex = html.indexOf(">", startIndex) + 1;

                // Ищем конец блока описания
                int endIndex = html.indexOf("<div class=\"vacancy-", startIndex);
                if (endIndex == -1) endIndex = html.indexOf("<section", startIndex);
                if (endIndex == -1) endIndex = startIndex + 10000; // Если текст огромный
                if (endIndex > html.length()) endIndex = html.length();

                String rawHtml = html.substring(startIndex, endIndex);
                return cleanHtmlToText(rawHtml);
            }
        } catch (Exception e) {
            log.debug("Failed to fetch HTML description for {}: {}", url, e.getMessage());
        }
        return "Подробное описание и требования смотрите по ссылке.";
    }

    // Превращаем HTML в красивый текст с переносами строк и буллитами
    private String cleanHtmlToText(String html) {
        String text = html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n\n")
                .replaceAll("(?i)</li>", "\n")
                .replaceAll("(?i)<li>", "• ")
                .replaceAll("<[^>]*>", "") // Удаляем все остальные теги
                .replaceAll("&nbsp;", " ")
                .replaceAll("&quot;", "\"")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&laquo;", "«")
                .replaceAll("&raquo;", "»")
                .replaceAll("&mdash;", "—")
                .replaceAll("&ndash;", "-");

        return text.replaceAll("(?m)^[ \t]*\r?\n", "\n").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private BigDecimal parseSalaryToUsd(String salaryStr) {
        if (salaryStr == null || salaryStr.contains("не указана")) return null;
        try {
            // Выцепляем числа даже с пробелами (например, "150 000")
            Pattern p = Pattern.compile("\\d+([\\s\\u00A0]*\\d+)*");
            Matcher m = p.matcher(salaryStr);
            if (m.find()) {
                String cleanNum = m.group().replaceAll("[\\s\\u00A0]", "");
                BigDecimal value = new BigDecimal(cleanNum);
                if (salaryStr.contains("$") || salaryStr.toLowerCase().contains("usd")) {
                    return value;
                }
                return currencyService.convertToUsd(value, "RUB");
            }
        } catch (Exception e) {
            log.warn("Could not parse salary: {}", salaryStr);
        }
        return null;
    }

    private Grade detectGrade(String text) {
        if (text.contains("intern") || text.contains("стажер")) return Grade.INTERN;
        if (text.contains("junior") || text.contains("младший") || text.contains("джун")) return Grade.JUNIOR;
        if (text.contains("middle") || text.contains("средний") || text.contains("мидл")) return Grade.MIDDLE;
        if (text.contains("senior") || text.contains("старший") || text.contains("сеньор") || text.contains("синьор")) return Grade.SENIOR;
        if (text.contains("lead") || text.contains("лид") || text.contains("руководитель")) return Grade.LEAD;
        return Grade.UNKNOWN;
    }
}