package com.sw8080.lookeng.dto.response;

import com.sw8080.lookeng.dto.WordItemDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WordSearchResponseDto {

    private List<WordItemDto> content;
    private PageInfo pageInfo;

    @Getter
    @Builder
    public static class PageInfo {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
