package com.example.backend.repository;

import com.example.backend.model.ScreeningRecord;
import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ScreeningRepository extends JpaRepository<ScreeningRecord, Long> {

    // ── RECRUITER scope — only their own screenings ──────────────
    List<ScreeningRecord> findByUserOrderByScoreDesc(User user);

    List<ScreeningRecord> findByUserAndClassificationOrderByScoreDesc(
            User user, String classification);

    // ── ADMIN scope — see everything across all recruiters ───────
    @Query("SELECT r FROM ScreeningRecord r ORDER BY r.score DESC")
    List<ScreeningRecord> findAllOrderByScoreDesc();

    List<ScreeningRecord> findByClassificationOrderByScoreDesc(String classification);
}
