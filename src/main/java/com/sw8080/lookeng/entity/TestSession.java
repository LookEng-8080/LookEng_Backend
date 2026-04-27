package com.sw8080.lookeng.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TestSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private QuizType quizType;

    private int totalCount;
    private int currentIndex;
    private int correctCount;
    private int durationSec;

    @ManyToMany // 테스트에 사용될 단어들을 연결
    @JoinTable(name = "test_session_words")
    private List<Word> words = new ArrayList<>();

    public void submitAnswer(boolean isCorrect) {
        if (isCorrect) {
            this.correctCount++;
        }
        this.currentIndex++; // 다음 문제로 넘어가기
    }
    public void finishTest(int durationSec) {
        this.durationSec = durationSec;
    }
}
