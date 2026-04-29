package com.sw8080.lookeng.controller;

import com.sw8080.lookeng.ApiResponse;
import com.sw8080.lookeng.dto.request.TestAnswerRequestDto;
import com.sw8080.lookeng.dto.request.TestFinishRequestDto;
import com.sw8080.lookeng.dto.request.TestSessionRequestDto;
import com.sw8080.lookeng.dto.response.TestAnswerResponseDto;
import com.sw8080.lookeng.dto.response.TestFinishResponseDto;
import com.sw8080.lookeng.dto.response.TestHistoryResponseDto;
import com.sw8080.lookeng.dto.response.TestSessionResponseDto;
import com.sw8080.lookeng.service.TestSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test/sessions")
@RequiredArgsConstructor
public class TestSessionController {

    private final TestSessionService testSessionService;

    @PostMapping
    public ResponseEntity<ApiResponse<TestSessionResponseDto>> startSession(
            @Valid @RequestBody TestSessionRequestDto request,
            HttpServletRequest httpRequest) {

        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요합니다.", null));
        }

        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        TestSessionResponseDto data = testSessionService.startSession(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "테스트가 시작되었습니다.", data));
    }
    @PostMapping("/{sessionId}/answers")
    public ResponseEntity<ApiResponse<TestAnswerResponseDto>> submitAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody TestAnswerRequestDto request,
            HttpServletRequest httpRequest) {

        // 1. 로그인 확인 (401)
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요합니다.", null));
        }

        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        TestAnswerResponseDto data = testSessionService.submitAnswer(userId, sessionId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "답안이 제출되었습니다.", data));
    }
    @PostMapping("/{sessionId}/finish")
    public ResponseEntity<ApiResponse<TestFinishResponseDto>> finishSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody TestFinishRequestDto request,
            HttpServletRequest httpRequest) {

        // 1. 로그인 확인 (401)
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요합니다.", null));
        }

        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        TestFinishResponseDto data = testSessionService.finishSession(userId, sessionId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "테스트가 종료되었습니다.", data));
    }
    @GetMapping
    public ResponseEntity<ApiResponse<TestHistoryResponseDto>> getTestHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {

        // 1. 로그인 확인 (401)
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요합니다.", null));
        }

        // 2. 세션에서 로그인한 유저 ID 꺼내기
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");

        // 3. 비즈니스 로직 실행
        TestHistoryResponseDto data = testSessionService.getTestHistory(userId, page, size);

        // 4. 성공 응답 반환
        return ResponseEntity.ok(new ApiResponse<>(true, "테스트 기록 조회 성공", data));
    }
}