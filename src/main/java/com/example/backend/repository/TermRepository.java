package com.example.backend.repository;

import com.example.backend.entity.Term;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, Long> {

    List<Term> findBySessionIdOrderByNameAsc(Long sessionId);

    Optional<Term> findFirstByIsActiveTrue();

    Optional<Term> findFirstBySessionIdAndIsActiveTrue(Long sessionId);
}
