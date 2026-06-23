package com.example.backend.dto;

import java.util.List;

public record BulkScreeningSummary(
        Long jobOpeningId,
        int totalSubmitted,
        int successCount,
        int failureCount,
        List<BulkScreeningItem> items
) {}