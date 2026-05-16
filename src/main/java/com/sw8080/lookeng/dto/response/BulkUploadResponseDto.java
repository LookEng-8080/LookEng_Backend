package com.sw8080.lookeng.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BulkUploadResponseDto {
    private int totalRequested;
    private int successCount;
    private int failCount;
}
