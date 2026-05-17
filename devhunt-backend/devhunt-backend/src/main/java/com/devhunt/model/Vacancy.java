package com.devhunt.model;

import com.devhunt.model.enums.Grade;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vacancies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vacancy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(name = "salary_orig")
    private String salaryOrig;

    @Column(name = "salary_usd")
    private BigDecimal salaryUsd;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, unique = true, length = 500)
    private String url;

    @Column(nullable = false)
    private String source;

    private String city;

    private Boolean remote;

    @Enumerated(EnumType.STRING)
    private Grade grade;

    @Column(name = "active_status")
    @Builder.Default
    private Boolean activeStatus = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}