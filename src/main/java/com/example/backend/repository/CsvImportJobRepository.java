package com.example.backend.repository;

import com.example.backend.entity.CsvImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CsvImportJobRepository extends JpaRepository<CsvImportJob, Long> {
}
