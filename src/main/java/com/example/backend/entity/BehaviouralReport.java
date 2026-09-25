package com.example.backend.entity;

import com.example.backend.entity.base.AuditableEntity;
import com.example.backend.entity.enums.BehaviourPeriodType;
import com.example.backend.entity.enums.BehaviourRating;
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
@Table(name = "behavioural_reports")
public class BehaviouralReport extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "term_id", nullable = false)
    private Long termId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private BehaviourPeriodType periodType;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "punctuality", length = 30)
    private BehaviourRating punctuality;

    @Enumerated(EnumType.STRING)
    @Column(name = "neatness", length = 30)
    private BehaviourRating neatness;

    @Enumerated(EnumType.STRING)
    @Column(name = "cooperation", length = 30)
    private BehaviourRating cooperation;

    @Enumerated(EnumType.STRING)
    @Column(name = "responsibility", length = 30)
    private BehaviourRating responsibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "conduct", length = 30)
    private BehaviourRating conduct;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

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

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getTermId() {
        return termId;
    }

    public void setTermId(Long termId) {
        this.termId = termId;
    }

    public BehaviourPeriodType getPeriodType() {
        return periodType;
    }

    public void setPeriodType(BehaviourPeriodType periodType) {
        this.periodType = periodType;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public BehaviourRating getPunctuality() {
        return punctuality;
    }

    public void setPunctuality(BehaviourRating punctuality) {
        this.punctuality = punctuality;
    }

    public BehaviourRating getNeatness() {
        return neatness;
    }

    public void setNeatness(BehaviourRating neatness) {
        this.neatness = neatness;
    }

    public BehaviourRating getCooperation() {
        return cooperation;
    }

    public void setCooperation(BehaviourRating cooperation) {
        this.cooperation = cooperation;
    }

    public BehaviourRating getResponsibility() {
        return responsibility;
    }

    public void setResponsibility(BehaviourRating responsibility) {
        this.responsibility = responsibility;
    }

    public BehaviourRating getConduct() {
        return conduct;
    }

    public void setConduct(BehaviourRating conduct) {
        this.conduct = conduct;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
