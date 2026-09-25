package com.example.backend.entity;

import com.example.backend.entity.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "preschool_report_summaries")
public class PreschoolReportSummary extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "class_teacher_comment", columnDefinition = "TEXT")
    private String classTeacherComment;

    @Column(name = "head_teacher_remark", columnDefinition = "TEXT")
    private String headTeacherRemark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getClassTeacherComment() {
        return classTeacherComment;
    }

    public void setClassTeacherComment(String classTeacherComment) {
        this.classTeacherComment = classTeacherComment;
    }

    public String getHeadTeacherRemark() {
        return headTeacherRemark;
    }

    public void setHeadTeacherRemark(String headTeacherRemark) {
        this.headTeacherRemark = headTeacherRemark;
    }
}
