package com.example.backend.repository;

import com.example.backend.entity.SchoolClass;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    Optional<SchoolClass> findFirstByNameIgnoreCase(String name);
}
