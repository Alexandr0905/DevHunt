package com.devhunt.service;

import com.devhunt.model.Vacancy;
import com.devhunt.model.enums.Grade;
import com.devhunt.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final VacancyRepository vacancyRepository;
    private final CurrencyService currencyService;

    // Стек технологий для сканера
    private static final List<String> TECH_STACK = List.of(
            "Java", "Spring", "Docker", "Kubernetes", "SQL", "PostgreSQL",
            "Kafka", "Redis", "React", "Vue", "Angular", "TypeScript",
            "Python", "Django", "FastAPI", "Git", "CI/CD", "Microservices"
    );

    // Блок 1: Топ навыков
    public List<Map<String, Object>> getSkillStats() {
        List<String> descriptions = vacancyRepository.findAllActiveDescriptions();
        Map<String, Long> counts = new HashMap<>();

        for (String desc : descriptions) {
            if (desc == null) continue;
            String lower = desc.toLowerCase();
            for (String tech : TECH_STACK) {
                if (lower.contains(tech.toLowerCase())) {
                    counts.put(tech, counts.getOrDefault(tech, 0L) + 1);
                }
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                // Вот тут добавили явное указание типов <String, Object> перед of
                .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue()))
                .collect(Collectors.toList());
    }

    // Блок 2 и 3: Направления + Грейды + ЗП
    public List<Map<String, Object>> getDirectionStats() {
        List<Vacancy> vacancies = vacancyRepository.findAllByActiveStatusTrue();
        long total = vacancies.size();

        Map<String, List<Vacancy>> grouped = vacancies.stream()
                .collect(Collectors.groupingBy(this::detectDirection));

        return grouped.entrySet().stream().map(entry -> {
            String dirName = entry.getKey();
            List<Vacancy> vList = entry.getValue();

            // Статистика по грейдам внутри направления
            Map<String, Map<String, Object>> gradeStats = new HashMap<>();
            for (Grade grade : Grade.values()) {
                if (grade == Grade.UNKNOWN) continue;

                List<Vacancy> byGrade = vList.stream().filter(v -> v.getGrade() == grade).toList();
                if (byGrade.isEmpty()) continue;

                double avgUsd = byGrade.stream()
                        .map(Vacancy::getSalaryUsd)
                        .filter(Objects::nonNull)
                        .mapToDouble(BigDecimal::doubleValue)
                        .average().orElse(0.0);

                BigDecimal avgRub = currencyService.convertToRub(BigDecimal.valueOf(avgUsd));

                gradeStats.put(grade.name(), Map.of(
                        "count", byGrade.size(),
                        "avgSalaryRub", avgRub.intValue()
                ));
            }

            Map<String, Object> res = new HashMap<>();
            res.put("name", dirName);
            res.put("count", vList.size());
            res.put("percent", total > 0 ? (vList.size() * 100.0) / total : 0);
            res.put("grades", gradeStats);
            return res;
        }).sorted((a, b) -> (Integer)b.get("count") - (Integer)a.get("count")).collect(Collectors.toList());
    }

    private String detectDirection(Vacancy v) {
        String t = v.getTitle().toLowerCase();
        if (t.contains("java") && !t.contains("script")) return "Java";
        if (t.contains("python")) return "Python";
        if (t.contains("frontend") || t.contains("react")) return "Frontend";
        if (t.contains("backend")) return "Backend";
        if (t.contains("qa") || t.contains("тестир")) return "QA";
        if (t.contains("devops")) return "DevOps";
        if (t.contains("data") || t.contains("analyst")) return "Data Science";
        if (t.contains("design") || t.contains("дизайн")) return "Design";
        return "Другое";
    }
}