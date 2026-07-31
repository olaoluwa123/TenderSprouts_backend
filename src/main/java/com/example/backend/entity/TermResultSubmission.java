package com.example.backend.entity;

import com.example.backend.entity.base.AuditableEntity;
import com.example.backend.entity.enums.TermResultSubmissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "term_result_submissions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_term_result_submissions_class_term", columnNames = {"class_id", "term_id"})
})
public class TermResultSubmission extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "term_id", nullable = false)
    private Long termId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TermResultSubmissionStatus status = TermResultSubmissionStatus.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "submitted_by_teacher_id")
    private Long submittedByTeacherId;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public TermResultSubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(TermResultSubmissionStatus status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Long getSubmittedByTeacherId() {
        return submittedByTeacherId;
    }

    public void setSubmittedByTeacherId(Long submittedByTeacherId) {
        this.submittedByTeacherId = submittedByTeacherId;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
