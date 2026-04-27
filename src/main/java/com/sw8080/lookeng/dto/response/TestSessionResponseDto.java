package com.sw8080.lookeng.dto.response;

import com.sw8080.lookeng.entity.QuizType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TestSessionResponseDto {
    private Long sessionId;
    private QuizType quizType;
    private int totalCount;
    private int currentIndex;
    private QuestionDto question;

    @Getter
    @Builder
    public static class QuestionDto {
        private Long wordId;
        private String korean;
        private String partOfSpeech;
        private String exampleSentence;
    }
}
