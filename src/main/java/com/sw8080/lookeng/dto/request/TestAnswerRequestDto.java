package com.sw8080.lookeng.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TestAnswerRequestDto {

    @NotNull(message = "단어 ID는 필수입니다.")
    private Long wordId;

    private String userInput;
}
