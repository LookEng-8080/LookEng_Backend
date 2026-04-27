package com.sw8080.lookeng.dto.response;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WordUpdateRequestDto {

    @Size(min = 1, max = 100, message = "영단어는 1자 이상 100자 이하이어야 합니다.")
    private String english;

    @Size(min = 1, max = 255, message = "뜻은 1자 이상 255자 이하이어야 합니다.")
    private String korean;

    @Size(max = 50)
    private String partOfSpeech;

    private String exampleSentence;

    @Size(max = 500)
    private String pronunciationUrl;

    // 명세서 400 에러: 아무 필드도 보내지 않은 '빈 깡통' 요청인지 확인하는 메서드
    public boolean isEmpty() {
        return english == null && korean == null && partOfSpeech == null
                && exampleSentence == null && pronunciationUrl == null;
    }
}
