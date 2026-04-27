package com.sw8080.lookeng.dto.response;

import com.sw8080.lookeng.dto.TestHistoryItemDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TestHistoryResponseDto {
    private List<TestHistoryItemDto> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;
}
