package com.sw8080.lookeng.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialLoginResponseDto {
    private Long userId;
    private String email;
    private String nickname;
    private String role;
    private Boolean isNewUser;
}
