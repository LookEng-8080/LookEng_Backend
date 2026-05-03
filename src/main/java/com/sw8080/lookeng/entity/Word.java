package com.sw8080.lookeng.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) // 생성/수정 시간 자동 기록
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String english;

    @Column(nullable = false, length = 255)
    private String korean;

    @Column(length = 50)
    private String partOfSpeech;

    @Column(columnDefinition = "TEXT")
    private String exampleSentence;

    @Column(length = 500)
    private String pronunciationUrl;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Builder
    public Word(String english, String korean, String partOfSpeech, String exampleSentence, String pronunciationUrl) {
        this.english = english;
        this.korean = korean;
        this.partOfSpeech = partOfSpeech;
        this.exampleSentence = exampleSentence;
        this.pronunciationUrl = pronunciationUrl;
    }
    public void update(String english, String korean, String partOfSpeech, String exampleSentence, String pronunciationUrl) {
        if (english != null) this.english = english;
        if (korean != null) this.korean = korean;
        if (partOfSpeech != null) this.partOfSpeech = partOfSpeech;
        if (exampleSentence != null) this.exampleSentence = exampleSentence;
        if (pronunciationUrl != null) this.pronunciationUrl = pronunciationUrl;
    }

    public void delete() {
        this.isDeleted = true;
    }
}
