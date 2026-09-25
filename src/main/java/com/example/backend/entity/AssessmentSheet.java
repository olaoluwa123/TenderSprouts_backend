package com.example.backend.entity;

import com.example.backend.entity.base.AuditableEntity;
import com.example.backend.entity.enums.AssessmentSheetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "assessment_sheets")
public class AssessmentSheet extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sheet_type", nullable = false, length = 20)
    private AssessmentSheetType sheetType;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "term_id", nullable = false)
    private Long termId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /** 1–16 for weekly tests; 0 for midterm. */
    @Column(name = "week_number", nullable = false)
    private Integer weekNumber = 0;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AssessmentSheetType getSheetType() {
        return sheetType;
    }

    public void setSheetType(AssessmentSheetType sheetType) {
        this.sheetType = sheetType;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getTermId() {
        return termId;
    }

    public void setTermId(Long termId) {
        this.termId = termId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
