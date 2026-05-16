package com.sw8080.lookeng.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemorizeResponseDto {
    private Long wordId;
    private Boolean isMemorized;
    private LocalDateTime memorizedAt;

    public static MemorizeResponseDto from(Long wordId, boolean isMemorized, LocalDateTime memorizedAt) {
        return MemorizeResponseDto.builder()
                .wordId(wordId)
                .isMemorized(isMemorized)
                .memorizedAt(memorizedAt)
                .build();
    }
}
