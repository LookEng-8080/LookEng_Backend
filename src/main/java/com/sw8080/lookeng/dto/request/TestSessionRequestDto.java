package com.sw8080.lookeng.dto.request;

import com.sw8080.lookeng.entity.QuizType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TestSessionRequestDto {
    private QuizType quizType;
    private int totalCount;
}
