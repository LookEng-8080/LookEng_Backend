package com.sw8080.lookeng.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressResponseDto {

    private int level;
    private long totalWords;
    private long masteredWords;
    private long wordsToNextLevel;

    public static ProgressResponseDto from(int level, long totalWords, long masteredWords, long wordsToNextLevel) {
        return ProgressResponseDto.builder()
                .level(level)
                .totalWords(totalWords)
                .masteredWords(masteredWords)
                .wordsToNextLevel(wordsToNextLevel)
                .build();
    }
}
