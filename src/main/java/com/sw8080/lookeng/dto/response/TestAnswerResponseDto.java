package com.sw8080.lookeng.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TestAnswerResponseDto {
    private Long wordId;
    private String userInput;
    private String correctAnswer;
    private boolean isCorrect;
    private int currentCorrectCount;
    private int answeredCount;
    private int totalCount;
    private boolean isFinished;
    private TestSessionResponseDto.QuestionDto nextQuestion; // 다음 문제
}
