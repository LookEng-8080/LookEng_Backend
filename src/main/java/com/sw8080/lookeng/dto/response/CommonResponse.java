package com.sw8080.lookeng.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommonResponse<T> {
    private boolean success; // 요청 성공 여부 (true/false)
    private String message;  // 프론트엔드에 띄워줄 안내 메시지
    private T data;          // 실제 전달할 데이터 (DTO 등)
}