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

    @ManyToMany
    @JoinTable(name = "test_session_words")
    @Builder.Default
    private List<Word> words = new ArrayList<>();

    @Column(columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean finished;

    public void submitAnswer(boolean isCorrect) {
        if (isCorrect) {
            this.correctCount++;
        }
        this.currentIndex++;
    }

    public void finishTest(int durationSec) {
        this.durationSec = durationSec;
        this.finished = true;
    }

    public boolean isAllAnswered() {
        return this.currentIndex >= this.totalCount;
    }
}
