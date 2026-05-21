package com.sw8080.lookeng.dto.response;

import com.sw8080.lookeng.entity.QuizType;
import com.sw8080.lookeng.entity.TestAnswer;
import com.sw8080.lookeng.entity.TestSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSessionDetailResponseDto {

    private Long sessionId;
    private QuizType quizType;
    private int score;
    private int totalQuestions;
    private int correctCount;
    private int incorrectCount;
    private int elapsedSeconds;
    private LocalDateTime createdAt;
    private List<AnswerDto> answers;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerDto {
        private int sequence;
        private Long wordId;
        private String english;
        private String korean;
        private String userAnswer;
        private boolean isCorrect;

        public static AnswerDto from(TestAnswer answer, int sequence) {
            return AnswerDto.builder()
                    .sequence(sequence)
                    .wordId(answer.getWord().getId())
                    .english(answer.getWord().getEnglish())
                    .korean(answer.getWord().getKorean())
                    .userAnswer(answer.getUserInput())
                    .isCorrect(answer.isCorrect())
                    .build();
        }
    }

    public static TestSessionDetailResponseDto from(TestSession session, List<TestAnswer> answers) {
        AtomicInteger seq = new AtomicInteger(1);
        List<AnswerDto> answerDtos = answers.stream()
                .map(a -> AnswerDto.from(a, seq.getAndIncrement()))
                .collect(Collectors.toList());

        return TestSessionDetailResponseDto.builder()
                .sessionId(session.getId())
                .quizType(session.getQuizType())
                .score(session.getCorrectCount())
                .totalQuestions(session.getTotalCount())
                .correctCount(session.getCorrectCount())
                .incorrectCount(session.getTotalCount() - session.getCorrectCount())
                .elapsedSeconds(session.getDurationSec())
                .createdAt(session.getCreatedAt())
                .answers(answerDtos)
                .build();
    }
}
