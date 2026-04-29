package com.sw8080.lookeng.dto.request;

import com.sw8080.lookeng.entity.QuizType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TestSessionRequestDto {

    @NotNull(message = "퀴즈 유형은 필수입니다.")
    private QuizType quizType;

    @Min(value = 1, message = "최소 1개 이상의 단어를 선택해야 합니다.")
    @Max(value = 50, message = "최대 50개까지 선택 가능합니다.")
    private int totalCount;
}
