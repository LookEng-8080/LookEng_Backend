package com.sw8080.lookeng.dto;

import com.sw8080.lookeng.entity.Word;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WordItemDto {
    private Long id;
    private String english;
    private String korean;
    private String partOfSpeech;
    private String exampleSentence;
    private String pronunciationUrl;

    private Boolean isMemorized;
    private Boolean isBookmarked;

    public static WordItemDto from(Word word, boolean isMemorized, boolean isBookmarked) {
        return WordItemDto.builder()
                .id(word.getId())
                .english(word.getEnglish())
                .korean(word.getKorean())
                .partOfSpeech(word.getPartOfSpeech())
                .exampleSentence(word.getExampleSentence())
                .pronunciationUrl(word.getPronunciationUrl())
                .isMemorized(isMemorized)
                .isBookmarked(isBookmarked)
                .build();
    }
}
