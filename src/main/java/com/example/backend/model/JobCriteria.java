package com.example.backend.model;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record JobCriteria(
        @NotBlank String jobTitle,
        @NotEmpty List<String> requiredSkills,
        @Min(0) int minYearsExperience
) {}
