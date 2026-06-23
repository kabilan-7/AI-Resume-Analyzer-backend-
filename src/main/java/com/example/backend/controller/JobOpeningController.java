package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.model.*;
import com.example.backend.repository.JobOpeningRepository;
import com.example.backend.repository.ScreeningRepository;
import com.example.backend.service.BulkScreeningService;
import com.example.backend.service.ExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobOpeningController {

    private final JobOpeningRepository jobOpeningRepository;
    private final ScreeningRepository screeningRepository;
    private final BulkScreeningService bulkScreeningService;
    private final ExportService exportService;

    private static final int MAX_BULK_FILES = 25;


    @PostMapping
    public ResponseEntity<JobOpening> createJob(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody JobOpeningRequest request) {

        JobOpening job = JobOpening.builder()
                .title(request.title())
                .requiredSkills(request.requiredSkills())
                .minYearsExperience(request.minYearsExperience())
                .description(request.description())
                .createdBy(currentUser)
                .build();

        jobOpeningRepository.save(job);
        return ResponseEntity.ok(job);
    }

    @GetMapping
    public ResponseEntity<List<JobOpening>> listJobs(
            @AuthenticationPrincipal User currentUser) {

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        List<JobOpening> jobs = isAdmin
                ? jobOpeningRepository.findAllByOrderByCreatedAtDesc()
                : jobOpeningRepository.findByCreatedByOrderByCreatedAtDesc(currentUser);

        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobOpening> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(findJobOrThrow(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        JobOpening job = findJobOrThrow(id);
        jobOpeningRepository.delete(job);
        return ResponseEntity.noContent().build();
    }

    // ════════════════════════════════════════════════════════════
    // Ranked candidates for one job
    // ════════════════════════════════════════════════════════════

    @GetMapping("/{id}/candidates")
    public ResponseEntity<List<ScreeningRecord>> getCandidates(@PathVariable Long id) {
        JobOpening job = findJobOrThrow(id);
        List<ScreeningRecord> candidates = screeningRepository.findByJobOpeningOrderByScoreDesc(job);
        return ResponseEntity.ok(candidates);
    }

    // ════════════════════════════════════════════════════════════
    // Bulk screening
    // ════════════════════════════════════════════════════════════

    @PostMapping(value = "/{id}/bulk-screen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkScreeningSummary> bulkScreen(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestPart("files") List<MultipartFile> files) {

        JobOpening job = findJobOrThrow(id);

        if (files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files uploaded");
        }
        if (files.size() > MAX_BULK_FILES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Max " + MAX_BULK_FILES + " files per bulk upload");
        }

        JobCriteria criteria = new JobCriteria(
                job.getTitle(), job.getRequiredSkills(), job.getMinYearsExperience());

        BulkScreeningSummary summary =
                bulkScreeningService.screenBulk(files, job, criteria, currentUser);

        return ResponseEntity.ok(summary);
    }

    // ════════════════════════════════════════════════════════════
    // Export
    // ════════════════════════════════════════════════════════════

    @GetMapping("/{id}/export/csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable Long id) {
        JobOpening job = findJobOrThrow(id);
        List<ScreeningRecord> candidates = screeningRepository.findByJobOpeningOrderByScoreDesc(job);

        String csv = exportService.generateCsv(job, candidates);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        String filename = sanitizeFilename(job.getTitle()) + "-candidates.csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        JobOpening job = findJobOrThrow(id);
        List<ScreeningRecord> candidates = screeningRepository.findByJobOpeningOrderByScoreDesc(job);

        try {
            byte[] pdfBytes = exportService.generatePdf(job, candidates);
            String filename = sanitizeFilename(job.getTitle()) + "-candidates.pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (IOException e) {
            log.error("PDF generation failed", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate PDF: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════

    private JobOpening findJobOrThrow(Long id) {
        return jobOpeningRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No job opening found with id: " + id));
    }

    private String sanitizeFilename(String title) {
        return title.replaceAll("[^a-zA-Z0-9\\-_]", "_").toLowerCase();
    }
}
