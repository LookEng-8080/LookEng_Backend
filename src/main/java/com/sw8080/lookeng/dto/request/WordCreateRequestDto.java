package com.sw8080.lookeng.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WordCreateRequestDto {
    @NotBlank(message = "영단어는 필수입니다.")
    @Size(max = 100, message = "영단어는 최대 100자까지 가능합니다.")
    private String english;

    @NotBlank(message = "뜻은 필수입니다.")
    @Size(max = 255, message = "뜻은 최대 255자까지 가능합니다.")
    private String korean;

    @Size(max = 50)
    private String partOfSpeech;

    private String exampleSentence;

    @Size(max = 500)
    private String pronunciationUrl;
}