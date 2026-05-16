package com.sw8080.lookeng.dto.response;

import com.sw8080.lookeng.entity.UserWord;
import com.sw8080.lookeng.entity.Word;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemorizedWordDto {
    private Long wordId;
    private String english;
    private String korean;
    private String partOfSpeech;
    private Boolean isMemorized;
    private LocalDateTime memorizedAt;
    private Boolean isBookmarked;

    public static MemorizedWordDto from(Word word, UserWord userWord) {
        return MemorizedWordDto.builder()
                .wordId(word.getId())
                .english(word.getEnglish())
                .korean(word.getKorean())
                .partOfSpeech(word.getPartOfSpeech())
                .isMemorized(true)
                .memorizedAt(userWord.getMemorizedAt())
                .isBookmarked(userWord.isBookmarked())
                .build();
    }
}
