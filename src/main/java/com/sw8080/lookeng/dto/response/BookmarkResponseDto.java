package com.sw8080.lookeng.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookmarkResponseDto {
    private Long wordId;
    private Boolean isBookmarked;

    public static BookmarkResponseDto from(Long wordId, boolean isBookmarked) {
        return BookmarkResponseDto.builder()
                .wordId(wordId)
                .isBookmarked(isBookmarked)
                .build();
    }
}
