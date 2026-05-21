package com.devhunt.controller;

import com.devhunt.dto.UserProfileDto;
import com.devhunt.model.User;
import com.devhunt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

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
        // Не отдаем хеш пароля на фронт ради безопасности
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }
}