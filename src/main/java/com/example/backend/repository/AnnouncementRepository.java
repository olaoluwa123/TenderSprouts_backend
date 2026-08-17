package com.example.backend.repository;

import com.example.backend.entity.Announcement;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findTop5ByIsImportantTrueOrderByCreatedAtDesc();

    List<Announcement> findTop5ByOrderByCreatedAtDesc();

    List<Announcement> findAllByOrderByCreatedAtDesc();

    List<Announcement> findTop50ByOrderByCreatedAtDesc();

    List<Announcement> findByCreatedByOrderByCreatedAtDesc(Long createdBy);

    @Query("""
        SELECT DISTINCT a FROM Announcement a
        LEFT JOIN a.recipients r
        WHERE a.audience = com.example.backend.entity.enums.AnnouncementAudience.TEACHERS
           OR (r.recipientType = com.example.backend.entity.enums.AnnouncementRecipientType.TEACHER
               AND r.recipientId = :teacherId)
        ORDER BY a.createdAt DESC
        """)
    List<Announcement> findTeacherInbox(@Param("teacherId") Long teacherId);

    @Query("""
        SELECT DISTINCT a FROM Announcement a
        LEFT JOIN a.recipients r
        WHERE a.audience = com.example.backend.entity.enums.AnnouncementAudience.ALL_PARENTS
           OR (a.audience = com.example.backend.entity.enums.AnnouncementAudience.CLASS
               AND a.classId IN :classIds)
           OR (a.audience = com.example.backend.entity.enums.AnnouncementAudience.SPECIFIC_PARENTS
               AND r.recipientType = com.example.backend.entity.enums.AnnouncementRecipientType.PARENT
               AND r.recipientId = :parentId)
           OR (a.audience = com.example.backend.entity.enums.AnnouncementAudience.SPECIFIC_PUPILS
               AND r.recipientType = com.example.backend.entity.enums.AnnouncementRecipientType.PUPIL
               AND r.recipientId IN :studentIds)
        ORDER BY a.createdAt DESC
        """)
    List<Announcement> findParentInbox(
        @Param("parentId") Long parentId,
        @Param("classIds") Collection<Long> classIds,
        @Param("studentIds") Collection<Long> studentIds);
}
