package com.example.backend.service;

import com.example.backend.dto.BulkScreeningItem;
import com.example.backend.dto.BulkScreeningSummary;
import com.example.backend.model.*;
import com.example.backend.repository.ScreeningRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class BulkScreeningService {

    private final TextExtractionService textExtractor;
    private final ResumeClassifierService classifier;
    private final ScreeningRepository screeningRepository;
    private final Executor screeningExecutor;

    public BulkScreeningService(
            TextExtractionService textExtractor,
            ResumeClassifierService classifier,
            ScreeningRepository screeningRepository,
            @Qualifier("screeningExecutor") Executor screeningExecutor) {
        this.textExtractor       = textExtractor;
        this.classifier          = classifier;
        this.screeningRepository = screeningRepository;
        this.screeningExecutor   = screeningExecutor;
    }

    public BulkScreeningSummary screenBulk(
            List<MultipartFile> files,
            JobOpening jobOpening,
            JobCriteria criteria,
            User currentUser) {

        log.info("Bulk screening: {} files for job '{}'",
                files.size(), jobOpening.getTitle());

        // ── Extract everything needed BEFORE spawning threads ──────
        // Worker threads run after the Hibernate session closes.
        // Capture only plain Java values here — no entity references.
        Long   jobId    = jobOpening.getId();
        String jobTitle = jobOpening.getTitle();
        Long   userId   = currentUser.getId();

        // Fan out
        List<CompletableFuture<BulkScreeningItem>> futures = files.stream()
                .map(file -> CompletableFuture.supplyAsync(
                        () -> screenOne(file, jobId, jobTitle, userId, criteria),
                        screeningExecutor))
                .toList();

        // Fan in
        List<BulkScreeningItem> items = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        int successCount = (int) items.stream()
                .filter(BulkScreeningItem::success).count();

        log.info("Bulk screening complete: {}/{} succeeded",
                successCount, files.size());

        return new BulkScreeningSummary(
                jobId,
                files.size(),
                successCount,
                files.size() - successCount,
                items
        );
    }

    // ── Worker method — runs on a pool thread ──────────────────────
    // Receives only plain Java primitives and records — no JPA entities.
    // This avoids LazyInitializationException entirely.
    private BulkScreeningItem screenOne(
            MultipartFile file,
            Long jobId,
            String jobTitle,
            Long userId,
            JobCriteria criteria) {

        String fileName = file.getOriginalFilename();

        try {
            // 1. Extract text
            String text = textExtractor.extract(file);

            // 2. Classify with AI
            ScreeningResult result = classifier.classify(text, criteria);

            // 3. Build entity using only IDs — no lazy loading needed
            ScreeningRecord record = new ScreeningRecord();
            record.setFileName(fileName);
            record.setJobTitle(jobTitle);
            record.setClassification(result.classification());
            record.setScore(result.score());
            record.setYearsExperience(result.yearsExperience());
            record.setMatchedSkills(result.matchedSkills());
            record.setMissingSkills(result.missingSkills());
            record.setSummary(result.summary());

            // Use proxy references by ID — no DB lookup, no lazy loading
            User userRef = new User();
            userRef.setId(userId);

            JobOpening jobRef = new JobOpening();
            jobRef.setId(jobId);

            record.setUser(userRef);
            record.setJobOpening(jobRef);

            screeningRepository.save(record);

            return new BulkScreeningItem(fileName, true, result, null);

        } catch (Exception e) {
            log.error("Failed to screen '{}': {}", fileName, e.getMessage());
            return new BulkScreeningItem(fileName, false, null, e.getMessage());
        }
    }
}