package com.devhunt.repository;

import com.devhunt.model.Vacancy;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VacancyRepository extends JpaRepository<Vacancy, Long>, JpaSpecificationExecutor<Vacancy> {
    Optional<Vacancy> findByUrl(String url);
    @Modifying
    @Transactional
    @Query("UPDATE Vacancy v SET v.activeStatus = false, v.deletedAt = CURRENT_TIMESTAMP " +
            "WHERE v.source = :source AND v.activeStatus = true")
    int markAllInactiveBySource(@Param("source") String source);
}