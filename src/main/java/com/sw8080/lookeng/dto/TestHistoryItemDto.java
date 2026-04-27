package com.sw8080.lookeng.dto;

import com.sw8080.lookeng.entity.QuizType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TestHistoryItemDto {
    private Long sessionId;
    private QuizType quizType;
    private int totalCount;
    private int correctCount;
    private double accuracy;
    private int durationSec;
    private LocalDateTime startedAt;
}