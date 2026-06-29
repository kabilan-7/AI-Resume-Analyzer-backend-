package com.example.backend.repository;

import com.example.backend.model.JobOpening;
import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface JobOpeningRepository extends JpaRepository<JobOpening, Long> {

    // Single job — fetches requiredSkills + createdBy eagerly
    @Query("""
        SELECT j FROM JobOpening j
        LEFT JOIN FETCH j.requiredSkills
        LEFT JOIN FETCH j.createdBy
        WHERE j.id = :id
        """)
    Optional<JobOpening> findByIdWithSkills(@Param("id") Long id);

    // Recruiter's own jobs
    @Query("""
        SELECT DISTINCT j FROM JobOpening j
        LEFT JOIN FETCH j.requiredSkills
        LEFT JOIN FETCH j.createdBy
        WHERE j.createdBy = :createdBy
        ORDER BY j.createdAt DESC
        """)
    List<JobOpening> findByCreatedByOrderByCreatedAtDesc(
            @Param("createdBy") User createdBy);

    // Admin — all jobs
    @Query("""
        SELECT DISTINCT j FROM JobOpening j
        LEFT JOIN FETCH j.requiredSkills
        LEFT JOIN FETCH j.createdBy
        ORDER BY j.createdAt DESC
        """)
    List<JobOpening> findAllByOrderByCreatedAtDesc();
}