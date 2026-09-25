package com.example.backend.repository;

import com.example.backend.entity.Teacher;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByUserId(Long userId);

    List<Teacher> findByUserIdIn(Collection<Long> userIds);
}
