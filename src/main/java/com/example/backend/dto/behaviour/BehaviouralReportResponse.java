package com.example.backend.dto.behaviour;

import com.example.backend.entity.BehaviouralReport;
import com.example.backend.entity.enums.BehaviourPeriodType;
import com.example.backend.entity.enums.BehaviourRating;
import java.time.LocalDateTime;

public record BehaviouralReportResponse(
    Long id,
    Long studentId,
    String studentName,
    String admissionNumber,
    Long classId,
    Long termId,
    BehaviourPeriodType periodType,
    Integer weekNumber,
    BehaviourRating punctuality,
    BehaviourRating neatness,
    BehaviourRating cooperation,
    BehaviourRating responsibility,
    BehaviourRating conduct,
    String remarks,
    LocalDateTime publishedAt
) {
    public static BehaviouralReportResponse from(
        BehaviouralReport report,
        String studentName,
        String admissionNumber) {
        return new BehaviouralReportResponse(
            report.getId(),
            report.getStudentId(),
            studentName,
            admissionNumber,
            report.getClassId(),
            report.getTermId(),
            report.getPeriodType(),
            report.getWeekNumber(),
            report.getPunctuality(),
            report.getNeatness(),
            report.getCooperation(),
            report.getResponsibility(),
            report.getConduct(),
            report.getRemarks(),
            report.getPublishedAt());
    }
}
