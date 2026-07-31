package com.example.backend.repository;

import com.example.backend.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByRole(User.Role role, Pageable pageable);

    Page<User> findByIsActive(Boolean isActive, Pageable pageable);

    Page<User> findByRoleAndIsActive(User.Role role, Boolean isActive, Pageable pageable);

    long countByRoleAndIsActive(User.Role role, Boolean isActive);
}
