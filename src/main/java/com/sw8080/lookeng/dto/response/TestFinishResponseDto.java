package com.sw8080.lookeng.dto.response;

import com.sw8080.lookeng.entity.QuizType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TestFinishResponseDto {
    private Long sessionId;
    private QuizType quizType;
    private int totalCount;
    private int correctCount;
    private double accuracy;
    private int durationSec;
    private LocalDateTime startedAt;
    private List<WrongWordDto> wrongWords;

    @Getter
    @Builder
    public static class WrongWordDto {
        private Long wordId;
        private String english;
        private String korean;
        private String userInput;
    }
}