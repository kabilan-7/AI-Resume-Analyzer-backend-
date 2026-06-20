package com.example.backend.controller;

import com.example.backend.model.*;
import com.example.backend.repository.ScreeningRepository;
import com.example.backend.service.ResumeClassifierService;
import com.example.backend.service.TextExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/screen")
@RequiredArgsConstructor
public class ScreeningController {

    private final TextExtractionService textExtractor;
    private final ResumeClassifierService classifier;
    private final ScreeningRepository repository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ScreeningResult> screen(
            @AuthenticationPrincipal User currentUser,   // ← injected automatically from JWT
            @RequestPart("file") MultipartFile file,
            @RequestPart("jobTitle") String jobTitle,
            @RequestPart("requiredSkills") List<String> requiredSkills,
            @RequestPart("minYearsExperience") String minYearsExperience) {

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        JobCriteria criteria = new JobCriteria(
                jobTitle, requiredSkills, Integer.parseInt(minYearsExperience));

        try {
            log.info("User {} screening: {} for job: {}",
                    currentUser.getEmail(), file.getOriginalFilename(), jobTitle);

            String text = textExtractor.extract(file);
            ScreeningResult result = classifier.classify(text, criteria);

            ScreeningRecord record = ScreeningRecord.builder()
                    .fileName(file.getOriginalFilename())
                    .jobTitle(jobTitle)
                    .classification(result.classification())
                    .score(result.score())
                    .yearsExperience(result.yearsExperience())
                    .matchedSkills(result.matchedSkills())
                    .missingSkills(result.missingSkills())
                    .summary(result.summary())
                    .user(currentUser)   // ← ties this screening to the logged-in recruiter
                    .build();

            repository.save(record);

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            log.error("File extraction failed", e);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not read file: " + e.getMessage());
        }
    }

    /**
     * RECRUITER sees only their own screenings.
     * ADMIN sees everyone's screenings (checked via role).
     */
    @GetMapping("/results")
    public ResponseEntity<List<ScreeningRecord>> getResults(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String classification) {

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        List<ScreeningRecord> results;

        if (isAdmin) {
            // Admins can see everything across all recruiters
            results = classification != null
                    ? repository.findByClassificationOrderByScoreDesc(classification)
                    : repository.findAllOrderByScoreDesc();
        } else {
            // Recruiters only see their own screenings
            results = classification != null
                    ? repository.findByUserAndClassificationOrderByScoreDesc(currentUser, classification)
                    : repository.findByUserOrderByScoreDesc(currentUser);
        }

        return ResponseEntity.ok(results);
    }
}