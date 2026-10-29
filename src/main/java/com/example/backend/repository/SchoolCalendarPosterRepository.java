package com.example.backend.repository;

import com.example.backend.entity.SchoolCalendarPoster;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolCalendarPosterRepository extends JpaRepository<SchoolCalendarPoster, Long> {

    @Query("""
        SELECT p.originalFilename AS originalFilename,
               p.contentType AS contentType,
               p.createdAt AS createdAt,
               p.updatedAt AS updatedAt
        FROM SchoolCalendarPoster p
        WHERE p.id = :id
        """)
    Optional<CalendarPosterMetadataView> findMetadataById(@Param("id") Long id);

    interface CalendarPosterMetadataView {
        String getOriginalFilename();
        String getContentType();
        LocalDateTime getCreatedAt();
        LocalDateTime getUpdatedAt();
    }
}
