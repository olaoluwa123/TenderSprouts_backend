package com.example.backend.dto.dashboard;

public record SubmissionFunnel(
    long draft,
    long submitted,
    long published,
    long totalClasses
) {
}
