package com.sw8080.lookeng.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    private LocalDateTime usedAt;

    @Builder
    public PasswordResetToken(Integer token, User user, LocalDateTime expiredAt) {
        this.token = token;
        this.user = user;
        this.expiredAt = expiredAt;
    }

    public void useToken() {
        this.usedAt = LocalDateTime.now();
    }
}
