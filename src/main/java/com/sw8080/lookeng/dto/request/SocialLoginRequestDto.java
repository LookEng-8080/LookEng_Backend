package com.sw8080.lookeng.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SocialLoginRequestDto {
    @NotBlank(message = "idToken은 필수 필드입니다.")
    private String idToken;
}
