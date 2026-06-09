package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScreeningResult(
        String classification,       // STRONG_FIT | POSSIBLE_FIT | NOT_FIT
        int score,                   // 0–100
        List<String> matchedSkills,
        List<String> missingSkills,
        int yearsExperience,
        String summary
) {}
