package com.sw8080.lookeng.repository;

import com.sw8080.lookeng.entity.TestAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestAnswerRepository extends JpaRepository<TestAnswer, Long> {
    List<TestAnswer> findByTestSessionIdAndIsCorrectFalse(Long sessionId);
}

