package com.sw8080.lookeng.repository;

import com.sw8080.lookeng.entity.TestSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
    Page<TestSession> findByUserId(Long userId, Pageable pageable);
}