package com.sw8080.lookeng.controller;

import com.sw8080.lookeng.ApiResponse;
import com.sw8080.lookeng.dto.response.BookmarkResponseDto;
import com.sw8080.lookeng.dto.response.BookmarkedWordDto;
import com.sw8080.lookeng.dto.response.MemorizeResponseDto;
import com.sw8080.lookeng.dto.response.MemorizedWordDto;
import com.sw8080.lookeng.service.UserWordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user/words")
@RequiredArgsConstructor
public class UserWordController {

    private final UserWordService userWordService;

    @GetMapping("/bookmarked")
    public ResponseEntity<ApiResponse<List<BookmarkedWordDto>>> getBookmarkedWords(
            HttpServletRequest httpRequest) {

        // 1. 인증 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 2. 세션에서 userId 추출
        Long userId = Long.valueOf(session.getAttribute("LOGIN_USER_ID").toString());

        // 3. 비즈니스 로직 실행
        List<BookmarkedWordDto> data = userWordService.getBookmarkedWords(userId);

        // 4. 200 OK 응답 반환
        return ResponseEntity.ok(new ApiResponse<>(true, "북마크 단어 목록 조회 성공", data));
    }

    @GetMapping("/memorized")
    public ResponseEntity<ApiResponse<List<MemorizedWordDto>>> getMemorizedWords(
            HttpServletRequest httpRequest) {

        // 1. 인증 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 2. 세션에서 userId 추출
        Long userId = Long.valueOf(session.getAttribute("LOGIN_USER_ID").toString());

        // 3. 비즈니스 로직 실행
        List<MemorizedWordDto> data = userWordService.getMemorizedWords(userId);

        // 4. 200 OK 응답 반환
        return ResponseEntity.ok(new ApiResponse<>(true, "암기 완료 단어 목록 조회 성공", data));
    }

    @PatchMapping("/{wordId}/bookmark")
    public ResponseEntity<ApiResponse<BookmarkResponseDto>> toggleBookmark(
            @PathVariable Long wordId,
            HttpServletRequest httpRequest) {

        // 1. 인증 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 2. 세션에서 userId 추출
        Long userId = Long.valueOf(session.getAttribute("LOGIN_USER_ID").toString());

        // 3. 비즈니스 로직 실행
        BookmarkResponseDto data = userWordService.toggleBookmark(userId, wordId);

        // 4. 200 OK 응답 반환
        return ResponseEntity.ok(new ApiResponse<>(true, "북마크 상태가 변경되었습니다.", data));
    }

    @PatchMapping("/{wordId}/memorize")
    public ResponseEntity<ApiResponse<MemorizeResponseDto>> toggleMemorize(
            @PathVariable Long wordId,
            HttpServletRequest httpRequest) {

        // 1. 인증 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 2. 세션에서 userId 추출
        Long userId = Long.valueOf(session.getAttribute("LOGIN_USER_ID").toString());

        // 3. 비즈니스 로직 실행
        MemorizeResponseDto data = userWordService.toggleMemorize(userId, wordId);

        // 4. 200 OK 응답 반환
        return ResponseEntity.ok(new ApiResponse<>(true, "암기 상태가 변경되었습니다.", data));
    }
}
