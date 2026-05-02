package com.sw8080.lookeng.dto.response;

import com.sw8080.lookeng.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserItemDto {

    private Long id;
    private String email;
    private String nickname;
    private String role;
    private LocalDateTime createdAt;

    public static UserItemDto from(User user) {
        return UserItemDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
