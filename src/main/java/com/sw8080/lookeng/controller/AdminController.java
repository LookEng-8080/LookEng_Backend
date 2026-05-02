package com.sw8080.lookeng.controller;

import com.sw8080.lookeng.ApiResponse;
import com.sw8080.lookeng.dto.response.TestHistoryResponseDto;
import com.sw8080.lookeng.dto.response.UserListResponseDto;
import com.sw8080.lookeng.exception.NotFoundException;
import com.sw8080.lookeng.repository.UserRepository;
import com.sw8080.lookeng.service.AdminService;
import com.sw8080.lookeng.service.TestSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final TestSessionService testSessionService;
    private final UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<UserListResponseDto>> getUsers(HttpServletRequest httpRequest) {

        // 1. 로그인 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요합니다.", null));
        }

        // 2. ADMIN 권한 확인
        Object roleObj = session.getAttribute("LOGIN_USER_ROLE");
        String role = roleObj != null ? roleObj.toString() : "";
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "관리자 접근 권한이 없습니다.", null));
        }

        // 3. USER 롤 전체 목록 조회
        UserListResponseDto data = adminService.getUsers();
        return ResponseEntity.ok(new ApiResponse<>(true, "유저 목록 조회 성공", data));
    }

    @GetMapping("/users/{userId}/test-sessions")
    public ResponseEntity<ApiResponse<TestHistoryResponseDto>> getUserTestSessions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {

        // 1. 로그인 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요합니다.", null));
        }

        // 2. ADMIN 권한 확인
        Object roleObj = session.getAttribute("LOGIN_USER_ROLE");
        String role = roleObj != null ? roleObj.toString() : "";
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "관리자 접근 권한이 없습니다.", null));
        }

        // 3. 대상 유저 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("해당 유저를 찾을 수 없습니다.");
        }

        // 4. 기존 서비스 재사용
        TestHistoryResponseDto data = testSessionService.getTestHistory(userId, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "유저 테스트 기록 조회 성공", data));
    }
}
