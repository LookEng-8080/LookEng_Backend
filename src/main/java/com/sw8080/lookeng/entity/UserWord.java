package com.sw8080.lookeng.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "USER_WORD")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @Column(name = "is_bookmarked", nullable = false)
    private boolean isBookmarked = false;

    @Column(name = "is_memorized", nullable = false)
    private boolean isMemorized = false;

    @Column(name = "memorized_at")
    private LocalDateTime memorizedAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public UserWord(Long userId, Long wordId, boolean isBookmarked, boolean isMemorized, LocalDateTime memorizedAt) {
        this.userId = userId;
        this.wordId = wordId;
        this.isBookmarked = isBookmarked;
        this.isMemorized = isMemorized;
        this.memorizedAt = memorizedAt;
    }

    public void toggleBookmark() {
        this.isBookmarked = !this.isBookmarked;
    }

    public void toggleMemorize() {
        if (this.isMemorized) {
            this.isMemorized = false;
            this.memorizedAt = null;
        } else {
            this.isMemorized = true;
            this.memorizedAt = LocalDateTime.now();
        }
    }
}
