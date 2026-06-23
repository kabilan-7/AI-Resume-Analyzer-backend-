package com.example.backend.dto;

import com.example.backend.model.ScreeningResult;

public record BulkScreeningItem(
        String fileName,
        boolean success,
        ScreeningResult result,
        String errorMessage
) {}