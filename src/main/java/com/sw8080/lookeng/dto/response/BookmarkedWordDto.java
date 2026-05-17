package com.sw8080.lookeng.dto.response;

import com.sw8080.lookeng.entity.UserWord;
import com.sw8080.lookeng.entity.Word;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookmarkedWordDto {
    private Long wordId;
    private String english;
    private String korean;
    private String partOfSpeech;
    private String exampleSentence;
    private Boolean isMemorized;
    private Boolean isBookmarked;

    public static BookmarkedWordDto from(Word word, UserWord userWord) {
        return BookmarkedWordDto.builder()
                .wordId(word.getId())
                .english(word.getEnglish())
                .korean(word.getKorean())
                .partOfSpeech(word.getPartOfSpeech())
                .exampleSentence(word.getExampleSentence())
                .isMemorized(userWord.isMemorized())
                .isBookmarked(true)
                .build();
    }
}
