package com.sw8080.lookeng.controller;

import com.sw8080.lookeng.ApiResponse;
import com.sw8080.lookeng.dto.response.ProgressResponseDto;
import com.sw8080.lookeng.service.ProgressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    // 1. 내 단어 진도율 및 등급 조회 API
    @GetMapping
    public ResponseEntity<ApiResponse<ProgressResponseDto>> getMyProgress(HttpServletRequest httpRequest) {

        // 2. 세션 확인 및 미로그인 시 401 에러 반환
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 3. 세션에서 로그인한 유저 ID 안전하게 꺼내기
        Object userIdObj = session.getAttribute("LOGIN_USER_ID");
        Long userId = Long.valueOf(userIdObj.toString());

        // 4. 서비스 로직 호출
        ProgressResponseDto responseDto = progressService.getProgress(userId);

        // 5. 공통 응답 포맷으로 감싸서 200 OK 반환
        return ResponseEntity.ok(new ApiResponse<>(true, "진도율 조회를 성공했습니다.", responseDto));
    }
}
