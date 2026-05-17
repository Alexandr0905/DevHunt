package com.devhunt.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "search_keywords")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchKeyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String keyword;
}