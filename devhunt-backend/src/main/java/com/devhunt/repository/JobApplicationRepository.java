package com.devhunt.repository;

import com.devhunt.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findAllByUserId(Long userId);
    Optional<JobApplication> findByUserIdAndVacancyId(Long userId, Long vacancyId);
}