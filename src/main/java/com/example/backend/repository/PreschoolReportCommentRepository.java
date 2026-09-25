package com.example.backend.repository;

import com.example.backend.entity.PreschoolReportComment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreschoolReportCommentRepository extends JpaRepository<PreschoolReportComment, Long> {

    List<PreschoolReportComment> findByReportId(Long reportId);

    List<PreschoolReportComment> findByReportIdAndStudentId(Long reportId, Long studentId);

    Optional<PreschoolReportComment> findByReportIdAndStudentIdAndSubjectId(
        Long reportId, Long studentId, Long subjectId);

    List<PreschoolReportComment> findByStudentId(Long studentId);

    void deleteByReportIdAndStudentIdAndSubjectIdNotIn(Long reportId, Long studentId, List<Long> subjectIds);
}
