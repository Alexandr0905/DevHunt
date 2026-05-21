package com.devhunt.model;

import com.devhunt.model.enums.Grade;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(name = "email_notifications")
    @Builder.Default
    private Boolean emailNotifications = true;

    @Column(name = "daily_digest")
    @Builder.Default
    private Boolean dailyDigest = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_favorites",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "vacancy_id")
    )
    @Builder.Default
    private Set<Vacancy> favorites = new HashSet<>();

    @Column(name = "role", nullable = false)
    @Builder.Default
    private String role = "ROLE_USER";

    // --- НОВЫЕ ПОЛЯ ДЛЯ TELEGRAM И ПОДПИСОК ---

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    @Column(name = "telegram_notifications_enabled")
    private boolean telegramNotificationsEnabled = true;

    // Подписка на ключевые слова (те самые чипсы-профессии)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_subscribed_keywords",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "keyword_id")
    )
    private Set<SearchKeyword> subscribedKeywords = new java.util.HashSet<>();

    // Подписка на грейды (чипсы-грейды)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_subscribed_grades", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "grade")
    @Enumerated(EnumType.STRING)
    private Set<Grade> subscribedGrades = new java.util.HashSet<>();
}