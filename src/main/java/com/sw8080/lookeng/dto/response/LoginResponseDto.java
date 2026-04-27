package com.sw8080.lookeng.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponseDto {
    private Long userId;
    private String email;
    private String nickname;
    private String role;
}
