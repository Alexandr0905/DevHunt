package com.devhunt.repository;

import com.devhunt.model.Vacancy;
import com.devhunt.model.enums.Grade;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class VacancySpecification {

    public static Specification<Vacancy> filterBy(String keyword, String direction, BigDecimal minSalaryUsd, Grade grade) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. ДИНАМИЧЕСКОЕ НАПРАВЛЕНИЕ (Парсим строку прямо из селекта)
            if (direction != null && !direction.isBlank() && !direction.equals("Все направления")) {

                // Убираем спецсимволы и "опасные" общие слова, чтобы не искать всех подряд
                String cleanDir = direction.toLowerCase()
                        .replaceAll("[()/]", " ")
                        .replace("разработчик", "")
                        .replace("инженер", "")
                        .replace("общий", "")
                        .trim();

                // Бьем оставшуюся чистую строку на отдельные слова
                String[] dirWords = cleanDir.split("\\s+");
                List<Predicate> dirPredicates = new ArrayList<>();

                for (String word : dirWords) {
                    if (word.length() > 1) { // Игнорируем случайный мусор из 1 буквы
                        dirPredicates.add(cb.like(cb.lower(root.get("title")), "%" + word + "%"));
                    }
                }

                // Ищем ХОТЯ БЫ ОДНО совпадение из слов направления в заголовке (OR)
                if (!dirPredicates.isEmpty()) {
                    predicates.add(cb.or(dirPredicates.toArray(new Predicate[0])));
                }
            }

            // 2. КЛЮЧЕВЫЕ СЛОВА (Ищем "docker kuber" — ВСЕ слова должны быть в заголовке ИЛИ описании)
            if (keyword != null && !keyword.isBlank()) {
                String[] words = keyword.trim().split("\\s+");
                List<Predicate> wordPredicates = new ArrayList<>();

                for (String word : words) {
                    String pattern = "%" + word.toLowerCase() + "%";
                    Predicate inTitle = cb.like(cb.lower(root.get("title")), pattern);
                    Predicate inDescription = cb.like(cb.lower(root.get("description")), pattern);

                    // Конкретное слово может быть либо там, либо там
                    wordPredicates.add(cb.or(inTitle, inDescription));
                }
                // Но присутствовать должны ОБЯЗАТЕЛЬНО ВСЕ введенные слова (AND)
                predicates.add(cb.and(wordPredicates.toArray(new Predicate[0])));
            }

            // 3. ЗАРПЛАТА (Больше или равно)
            if (minSalaryUsd != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("salaryUsd"), minSalaryUsd));
            }

            // 4. ГРЕЙД
            if (grade != null && grade != Grade.UNKNOWN) {
                predicates.add(cb.equal(root.get("grade"), grade));
            }

            // 5. ТОЛЬКО АКТИВНЫЕ
            predicates.add(cb.isTrue(root.get("activeStatus")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}