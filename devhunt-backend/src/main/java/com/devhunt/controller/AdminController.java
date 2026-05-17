package com.devhunt.controller;

import com.devhunt.model.SearchKeyword;
import com.devhunt.model.User;
import com.devhunt.repository.SearchKeywordRepository;
import com.devhunt.repository.UserRepository;
import com.devhunt.service.VacancyScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserRepository userRepository;
    private final VacancyScheduler vacancyScheduler;
    private final SearchKeywordRepository searchKeywordRepository;

    // --- УПРАВЛЕНИЕ ПОЛЬЗОВАТЕЛЯМИ ---

    // 1. Получить всех пользователей
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 2. Изменить роль пользователя с защитой от самовыпила
    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestBody Map<String, String> body, Principal principal) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Юзер не найден"));

        // Защита: нельзя понизить самого себя
        if (user.getEmail().equals(principal.getName())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Вы не можете понизить в правах самого себя!"));
        }

        user.setRole(body.get("role"));
        userRepository.save(user);
        return ResponseEntity.ok(user);
    }

    // 3. Удалить пользователя с защитой от самовыпила
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Principal principal) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Юзер не найден"));

        // Защита: нельзя удалить самого себя
        if (user.getEmail().equals(principal.getName())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Вы не можете удалить свой аккаунт из админки!"));
        }

        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Пользователь удален"));
    }

    // --- УПРАВЛЕНИЕ СЛОВАРЕМ ПОИСКА ---

    @GetMapping("/keywords")
    public List<SearchKeyword> getKeywords() {
        return searchKeywordRepository.findAll();
    }

    @PostMapping("/keywords")
    public ResponseEntity<?> addKeyword(@RequestBody Map<String, String> body) {
        String word = body.get("keyword");
        if (word == null || word.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Слово не может быть пустым"));
        }
        SearchKeyword keyword = new SearchKeyword();
        keyword.setKeyword(word.trim());
        return ResponseEntity.ok(searchKeywordRepository.save(keyword));
    }

    @DeleteMapping("/keywords/{id}")
    public ResponseEntity<?> deleteKeyword(@PathVariable Long id) {
        searchKeywordRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // --- КНОПКА ЗАПУСКА ПАРСЕРОВ ---
    @PostMapping("/scrape")
    public ResponseEntity<?> triggerScrape() {
        // Запускаем парсинг в отдельном потоке, используя твой метод runScrapers
        new Thread(vacancyScheduler::runScrapers).start();
        return ResponseEntity.ok(Map.of("message", "Парсинг запущен в фоновом режиме!"));
    }
}