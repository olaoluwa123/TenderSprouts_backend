package com.example.backend.repository;

import com.example.backend.entity.Admin;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUserId(Long userId);
}
