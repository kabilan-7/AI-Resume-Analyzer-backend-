package com.example.backend.dto;

public record AuthResponse(
        String token,
        String email,
        String fullName,
        String role
) {}
