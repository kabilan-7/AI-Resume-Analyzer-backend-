package com.example.backend.repository;

import com.example.backend.model.JobOpening;
import com.example.backend.model.ScreeningRecord;
import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ScreeningRepository extends JpaRepository<ScreeningRecord, Long> {

    // ── RECRUITER scope ──────────────────────────────────────────
    List<ScreeningRecord> findByUserOrderByScoreDesc(User user);

    List<ScreeningRecord> findByUserAndClassificationOrderByScoreDesc(
            User user, String classification);

    // ── ADMIN scope ──────────────────────────────────────────────
    @Query("SELECT r FROM ScreeningRecord r ORDER BY r.score DESC")
    List<ScreeningRecord> findAllOrderByScoreDesc();

    List<ScreeningRecord> findByClassificationOrderByScoreDesc(String classification);

    // ── NEW — Job pipeline ranking ──────────────────────────────
    List<ScreeningRecord> findByJobOpeningOrderByScoreDesc(JobOpening jobOpening);
}