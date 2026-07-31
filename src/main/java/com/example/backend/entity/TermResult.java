package com.example.backend.entity;

import com.example.backend.entity.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "term_results", uniqueConstraints = {
    @UniqueConstraint(name = "uk_term_results_student_term", columnNames = {"student_id", "term_id"})
})
public class TermResult extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "term_id", nullable = false)
    private Long termId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "average_score", precision = 5, scale = 2)
    private BigDecimal averageScore;

    @Column(name = "rank_in_class")
    private Integer rankInClass;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public TermResult() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
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

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(BigDecimal averageScore) {
        this.averageScore = averageScore;
    }

    public Integer getRankInClass() {
        return rankInClass;
    }

    public void setRankInClass(Integer rankInClass) {
        this.rankInClass = rankInClass;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
