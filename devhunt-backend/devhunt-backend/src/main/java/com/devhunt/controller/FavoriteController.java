package com.devhunt.controller;

import com.devhunt.model.User;
import com.devhunt.model.Vacancy;
import com.devhunt.repository.UserRepository;
import com.devhunt.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final UserRepository userRepository;
    private final VacancyRepository vacancyRepository;

    @GetMapping
    public ResponseEntity<Set<Vacancy>> getFavorites(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(user.getFavorites());
    }

    @PostMapping("/{vacancyId}")
    public ResponseEntity<?> addFavorite(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long vacancyId) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        Vacancy vacancy = vacancyRepository.findById(vacancyId).orElseThrow(() -> new RuntimeException("Vacancy not found"));

        user.getFavorites().add(vacancy);
        userRepository.save(user);
        return ResponseEntity.ok("Added to favorites");
    }

    @DeleteMapping("/{vacancyId}")
    public ResponseEntity<?> removeFavorite(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long vacancyId) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        Vacancy vacancy = vacancyRepository.findById(vacancyId).orElseThrow(() -> new RuntimeException("Vacancy not found"));

        user.getFavorites().remove(vacancy);
        userRepository.save(user);
        return ResponseEntity.ok("Removed from favorites");
    }
}