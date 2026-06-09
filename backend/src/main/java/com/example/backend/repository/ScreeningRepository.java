package com.example.backend.repository;

import com.example.backend.model.ScreeningRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ScreeningRepository extends JpaRepository<ScreeningRecord, Long> {

    List<ScreeningRecord> findByClassificationOrderByScoreDesc(String classification);

    @Query("SELECT r FROM ScreeningRecord r ORDER BY r.score DESC")
    List<ScreeningRecord> findAllOrderByScoreDesc();
}
