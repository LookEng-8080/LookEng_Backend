package com.sw8080.lookeng.repository;

import com.sw8080.lookeng.entity.TestAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestAnswerRepository extends JpaRepository<TestAnswer, Long> {
    List<TestAnswer> findByTestSessionIdAndIsCorrectFalse(Long sessionId);

    @Query("SELECT COUNT(DISTINCT ta.word.id) FROM TestAnswer ta " +
            "JOIN ta.testSession ts " +
            "WHERE ts.userId = :userId AND ta.isCorrect = true")
    long countDistinctCorrectWordsByUserId(@Param("userId") Long userId);
}

