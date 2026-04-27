package com.sw8080.lookeng.dto.response;

import com.sw8080.lookeng.dto.WordItemDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WordListResponseDto {
    private List<WordItemDto> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;
}
