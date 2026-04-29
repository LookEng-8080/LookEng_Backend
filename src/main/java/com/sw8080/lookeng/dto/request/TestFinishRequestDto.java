package com.sw8080.lookeng.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TestFinishRequestDto {

    @NotNull(message = "소요 시간은 필수입니다.")
    @Min(value = 1, message = "소요 시간은 1초 이상이어야 합니다.")
    private Integer durationSec;
}
