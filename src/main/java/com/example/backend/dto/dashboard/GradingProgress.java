package com.example.backend.dto.dashboard;

public record GradingProgress(
    long enrolledStudents,
    long studentsWithGrades,
    long subjectCount,
    int percentComplete,
    String nextAction
) {
}
