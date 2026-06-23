package com.example.backend.service;

import com.example.backend.dto.BulkScreeningItem;
import com.example.backend.dto.BulkScreeningSummary;
import com.example.backend.model.*;
import com.example.backend.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
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
        this.textExtractor = textExtractor;
        this.classifier = classifier;
        this.screeningRepository = screeningRepository;
        this.screeningExecutor = screeningExecutor;
    }

    /**
     * Screens every file in parallel against the same job criteria.
     * Each resume is processed independently — one failure doesn't
     * block the rest of the batch.
     */
    public BulkScreeningSummary screenBulk(
            List<MultipartFile> files,
            JobOpening jobOpening,
            JobCriteria criteria,
            User currentUser) {

        log.info("Starting bulk screening: {} files for job '{}'",
                files.size(), jobOpening.getTitle());

        // Fan out — submit all files to the thread pool at once
        List<CompletableFuture<BulkScreeningItem>> futures = files.stream()
                .map(file -> CompletableFuture.supplyAsync(
                        () -> screenOne(file, jobOpening, criteria, currentUser),
                        screeningExecutor))
                .toList();

        // Fan in — wait for every result, in original submission order
        List<BulkScreeningItem> items = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        int successCount = (int) items.stream().filter(BulkScreeningItem::success).count();

        log.info("Bulk screening complete: {}/{} succeeded", successCount, files.size());

        return new BulkScreeningSummary(
                jobOpening.getId(),
                files.size(),
                successCount,
                files.size() - successCount,
                items
        );
    }

    /**
     * Runs on a worker thread from the pool. Extracts text, classifies
     * with the AI, and saves to the database — all independently of
     * any other resume in the batch.
     */
    private BulkScreeningItem screenOne(
            MultipartFile file,
            JobOpening jobOpening,
            JobCriteria criteria,
            User currentUser) {

        String fileName = file.getOriginalFilename();

        try {
            String text = textExtractor.extract(file);
            ScreeningResult result = classifier.classify(text, criteria);

            ScreeningRecord record = ScreeningRecord.builder()
                    .fileName(fileName)
                    .jobTitle(jobOpening.getTitle())
                    .classification(result.classification())
                    .score(result.score())
                    .yearsExperience(result.yearsExperience())
                    .matchedSkills(result.matchedSkills())
                    .missingSkills(result.missingSkills())
                    .summary(result.summary())
                    .user(currentUser)
                    .jobOpening(jobOpening)
                    .build();

            screeningRepository.save(record);

            return new BulkScreeningItem(fileName, true, result, null);

        } catch (Exception e) {
            log.error("Failed to screen '{}': {}", fileName, e.getMessage());
            return new BulkScreeningItem(fileName, false, null, e.getMessage());
        }
    }
}