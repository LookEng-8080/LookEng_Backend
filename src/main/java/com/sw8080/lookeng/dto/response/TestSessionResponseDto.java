package com.sw8080.lookeng.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sw8080.lookeng.entity.QuizType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class QuestionDto {
        private Long wordId;
        private String korean;
        private String partOfSpeech;
        private String exampleSentence;
        private String sentence;         // FILL_IN_BLANK: 빈칸 처리된 예문
        private List<String> choices;    // MULTIPLE_CHOICE / FILL_IN_BLANK: 4지선다 보기
    }
}
