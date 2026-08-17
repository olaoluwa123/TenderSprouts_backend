package com.example.backend.repository;

import com.example.backend.entity.FeeTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeTemplateRepository extends JpaRepository<FeeTemplate, Long> {

    Optional<FeeTemplate> findByClassId(Long classId);
}
