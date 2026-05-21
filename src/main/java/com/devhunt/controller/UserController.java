package com.devhunt.controller;

import com.devhunt.dto.UserProfileDto;
import com.devhunt.model.User;
import com.devhunt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.devhunt.service.telegram.DevHuntBot;
import com.devhunt.dto.TelegramSettingsDto;
import com.devhunt.model.SearchKeyword;
import com.devhunt.repository.SearchKeywordRepository;
import com.devhunt.model.enums.Grade;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // ВАЖНО: Вот этой строчки не хватало!
    // Без неё Java не понимала, откуда брать ключевые слова.
    private final SearchKeywordRepository keywordRepository;

    @Value("${telegram.bot.name}")
    private String botName;

    @GetMapping
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(new UserProfileDto(user.getEmail(), user.getEmailNotifications(), user.getDailyDigest()));
    }

    @PutMapping
    public ResponseEntity<UserProfileDto> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserProfileDto dto) {

        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        user.setEmailNotifications(dto.emailNotifications());
        user.setDailyDigest(dto.dailyDigest());
        userRepository.save(user);

        return ResponseEntity.ok(new UserProfileDto(user.getEmail(), user.getEmailNotifications(), user.getDailyDigest()));
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Юзер не найден"));
        return ResponseEntity.ok(user);
    }

    @GetMapping("/telegram-status")
    public ResponseEntity<?> getTelegramStatus(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of(
                "connected", user.getTelegramChatId() != null,
                "enabled", user.isTelegramNotificationsEnabled()
        ));
    }

    @GetMapping("/telegram-token")
    public ResponseEntity<?> generateTelegramLink(Principal principal) {
        String email = principal.getName();
        String token = UUID.randomUUID().toString();

        DevHuntBot.pendingTokens.put(token, email);

        String link = "https://t.me/" + botName + "?start=" + token;

        return ResponseEntity.ok(Map.of("link", link));
    }

    @DeleteMapping("/telegram")
    public ResponseEntity<?> unlinkTelegram(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTelegramChatId(null);
        user.setTelegramNotificationsEnabled(false);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Telegram disconnected"));
    }

    @GetMapping("/telegram-settings")
    public ResponseEntity<TelegramSettingsDto> getTelegramSettings(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> allKeywords = keywordRepository.findAll().stream()
                .map(SearchKeyword::getKeyword)
                .collect(Collectors.toList());

        Set<String> selectedKeywords = user.getSubscribedKeywords().stream()
                .map(SearchKeyword::getKeyword)
                .collect(Collectors.toSet());

        Set<String> selectedGrades = user.getSubscribedGrades().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return ResponseEntity.ok(new TelegramSettingsDto(
                user.isTelegramNotificationsEnabled(),
                selectedKeywords,
                allKeywords,
                selectedGrades
        ));
    }

    @PutMapping("/telegram-settings")
    public ResponseEntity<?> updateTelegramSettings(Principal principal, @RequestBody TelegramSettingsDto dto) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTelegramNotificationsEnabled(dto.enabled());

        List<SearchKeyword> foundKeywords = keywordRepository.findAll().stream()
                .filter(k -> dto.selectedKeywords().contains(k.getKeyword()))
                .toList();

        user.getSubscribedKeywords().clear();
        user.getSubscribedKeywords().addAll(foundKeywords);

        user.getSubscribedGrades().clear();
        for (String g : dto.selectedGrades()) {
            try {
                user.getSubscribedGrades().add(Grade.valueOf(g));
            } catch (IllegalArgumentException ignored) {}
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Settings updated successfully"));
    }
}