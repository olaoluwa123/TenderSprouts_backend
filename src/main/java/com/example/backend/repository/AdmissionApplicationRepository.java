package com.example.backend.repository;

import com.example.backend.entity.AdmissionApplication;
import com.example.backend.entity.enums.AdmissionApplicationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionApplicationRepository extends JpaRepository<AdmissionApplication, Long> {

    long countByStatus(AdmissionApplicationStatus status);

    List<AdmissionApplication> findTop8ByStatusOrderByCreatedAtDesc(AdmissionApplicationStatus status);

    List<AdmissionApplication> findAllByOrderByCreatedAtDesc();
}
