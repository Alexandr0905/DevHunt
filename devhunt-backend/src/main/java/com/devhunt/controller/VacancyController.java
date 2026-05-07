package com.devhunt.controller;

import com.devhunt.model.Vacancy;
import com.devhunt.model.enums.Grade;
import com.devhunt.repository.VacancyRepository;
import com.devhunt.repository.VacancySpecification;
import com.devhunt.service.CurrencyService;
import com.devhunt.service.VacancyScheduler; // Обязательный импорт
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/vacancies")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class VacancyController {

    private final VacancyRepository vacancyRepository;
    private final VacancyScheduler vacancyScheduler; // Внедряем шедулер
    private final CurrencyService currencyService; // Внедряем сервис конвертации валют

    @GetMapping
    public Page<Vacancy> getVacancies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String direction, // Добавили Направление
            @RequestParam(required = false) BigDecimal minSalaryRub, // Оставили рубли
            @RequestParam(required = false) Grade grade,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Конвертируем рубли в доллары для БД
        BigDecimal minSalaryUsd = null;
        if (minSalaryRub != null && minSalaryRub.compareTo(BigDecimal.ZERO) > 0) {
            minSalaryUsd = currencyService.convertToUsd(minSalaryRub, "RUB");
        }

        // Передаем и keyword, и direction в нашу новую спецификацию
        Specification<Vacancy> spec = VacancySpecification.filterBy(keyword, direction, minSalaryUsd, grade);

        return vacancyRepository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    // Тот самый эндпоинт для ручного запуска парсера
    @GetMapping("/scrape-now")
    public ResponseEntity<String> forceScrape() {
        vacancyScheduler.runScrapers();
        return ResponseEntity.ok("Скрапинг успешно запущен! Проверь логи бэкенда.");
    }
}