package com.example.backend.controller;

import com.example.backend.model.*;
import com.example.backend.repository.ScreeningRepository;
import com.example.backend.service.ResumeClassifierService;
import com.example.backend.service.TextExtractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
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
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobTitle") String jobTitle,
            @RequestParam("requiredSkills") List<String> requiredSkills,
            @RequestParam("minYearsExperience") Integer minYearsExperience) {

        if (file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File is empty"
            );
        }

        JobCriteria criteria = new JobCriteria(
                jobTitle,
                requiredSkills,
                minYearsExperience
        );

        try {
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
                    .build();

            repository.save(record);

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not read file: " + e.getMessage()
            );
        }
    }

    @GetMapping("/results")
    public ResponseEntity<List<ScreeningRecord>> getResults(
            @RequestParam(required = false) String classification) {

        List<ScreeningRecord> results = classification != null
                ? repository.findByClassificationOrderByScoreDesc(classification)
                : repository.findAllOrderByScoreDesc();

        return ResponseEntity.ok(results);
    }
}
