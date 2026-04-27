package com.sw8080.lookeng.dto.response;

import com.sw8080.lookeng.entity.Word;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WordResponseDto {
    private Long id;
    private String english;
    private String korean;
    private String partOfSpeech;
    private String exampleSentence;
    private String pronunciationUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity를 DTO로 변환하는 편의 메서드
    public static WordResponseDto from(Word word) {
        return WordResponseDto.builder()
                .id(word.getId())
                .english(word.getEnglish())
                .korean(word.getKorean())
                .partOfSpeech(word.getPartOfSpeech())
                .exampleSentence(word.getExampleSentence())
                .pronunciationUrl(word.getPronunciationUrl())
                .createdAt(word.getCreatedAt())
                .updatedAt(word.getUpdatedAt())
                .build();
    }
}
