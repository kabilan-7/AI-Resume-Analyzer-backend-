package com.example.backend.repository;

import com.example.backend.model.JobOpening;
import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobOpeningRepository extends JpaRepository<JobOpening, Long> {

    List<JobOpening> findByCreatedByOrderByCreatedAtDesc(User createdBy);

    List<JobOpening> findAllByOrderByCreatedAtDesc();
}