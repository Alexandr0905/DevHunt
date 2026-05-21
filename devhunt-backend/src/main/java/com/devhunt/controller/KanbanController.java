package com.devhunt.controller;

import com.devhunt.model.JobApplication;
import com.devhunt.model.User;
import com.devhunt.model.Vacancy;
import com.devhunt.model.enums.ApplicationStatus;
import com.devhunt.repository.JobApplicationRepository;
import com.devhunt.repository.UserRepository;
import com.devhunt.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kanban")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class KanbanController {

    private final JobApplicationRepository applicationRepository;
    private final VacancyRepository vacancyRepository;
    private final UserRepository userRepository;

    // Получаем текущего юзера по email из токена
    private User getCurrentUser(Principal principal) {
        String email = principal.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    @GetMapping
    public List<JobApplication> getMyApplications(Principal principal) {
        User user = getCurrentUser(principal);
        return applicationRepository.findAllByUserId(user.getId());
    }

    @PostMapping("/apply/{vacancyId}")
    public ResponseEntity<?> applyForJob(Principal principal, @PathVariable Long vacancyId) {
        User user = getCurrentUser(principal);

        if (applicationRepository.findByUserIdAndVacancyId(user.getId(), vacancyId).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Уже в трекере"));
        }

        Vacancy vacancy = vacancyRepository.findById(vacancyId).orElseThrow();

        JobApplication app = JobApplication.builder()
                .user(user)
                .vacancy(vacancy)
                .status(ApplicationStatus.VIEWED)
                .build();

        applicationRepository.save(app);
        return ResponseEntity.ok(app);
    }

    @PutMapping("/{appId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long appId, @RequestBody Map<String, String> body) {
        JobApplication app = applicationRepository.findById(appId).orElseThrow();
        app.setStatus(ApplicationStatus.valueOf(body.get("status")));
        applicationRepository.save(app);
        return ResponseEntity.ok(app);
    }

    // 1. Удаление по ID приложения (для крестика на доске)
    @DeleteMapping("/{appId}")
    public ResponseEntity<?> deleteApplication(@PathVariable Long appId, Principal principal) {
        User user = getCurrentUser(principal);
        JobApplication app = applicationRepository.findById(appId).orElseThrow();

        // Проверка, что это именно его отклик
        if (!app.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        applicationRepository.delete(app);
        return ResponseEntity.ok().build();
    }

    // 2. Удаление по ID вакансии (для кнопки отмены в ленте)
    @DeleteMapping("/vacancy/{vacancyId}")
    public ResponseEntity<?> removeByVacancyId(@PathVariable Long vacancyId, Principal principal) {
        User user = getCurrentUser(principal);
        applicationRepository.findByUserIdAndVacancyId(user.getId(), vacancyId)
                .ifPresent(applicationRepository::delete);
        return ResponseEntity.ok().build();
    }
}