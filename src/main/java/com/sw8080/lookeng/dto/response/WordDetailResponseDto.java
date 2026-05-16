package com.sw8080.lookeng.dto.response;

import com.sw8080.lookeng.entity.Word;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WordDetailResponseDto {
    private Long id;
    private String english;
    private String korean;
    private String partOfSpeech;
    private String exampleSentence;
    private String pronunciationUrl;

    private Boolean isMemorized;
    private Boolean isBookmarked;

    // 시간 정보
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WordDetailResponseDto from(Word word, boolean isMemorized, boolean isBookmarked) {
        return WordDetailResponseDto.builder()
                .id(word.getId())
                .english(word.getEnglish())
                .korean(word.getKorean())
                .partOfSpeech(word.getPartOfSpeech())
                .exampleSentence(word.getExampleSentence())
                .pronunciationUrl(word.getPronunciationUrl())
                .isMemorized(isMemorized)
                .isBookmarked(isBookmarked)
                .createdAt(word.getCreatedAt())
                .updatedAt(word.getUpdatedAt())
                .build();
    }
}
