package com.example.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record JobOpeningRequest(

        @NotBlank
        String title,

        @NotEmpty
        List<String> requiredSkills,

        @Min(0)
        int minYearsExperience,

        String description
) {}