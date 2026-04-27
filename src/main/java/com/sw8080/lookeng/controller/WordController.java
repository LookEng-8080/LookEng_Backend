package com.sw8080.lookeng.controller;

import com.sw8080.lookeng.ApiResponse;
import com.sw8080.lookeng.dto.request.WordCreateRequestDto;
import com.sw8080.lookeng.dto.response.WordDetailResponseDto;
import com.sw8080.lookeng.dto.response.WordListResponseDto;
import com.sw8080.lookeng.dto.response.WordResponseDto;
import com.sw8080.lookeng.dto.response.WordUpdateRequestDto;
import com.sw8080.lookeng.service.WordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    @PostMapping
    public ResponseEntity<ApiResponse<WordResponseDto>> createWord(
            @Valid @RequestBody WordCreateRequestDto request,
            HttpServletRequest httpRequest) {

        // 1. 명세서 401 에러: 인증(로그인) 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 2. 명세서 403 에러: ADMIN 권한 확인
        // 주의: 세션에 저장된 Role이 Enum인 경우 문자열 변환 처리
        Object roleObj = session.getAttribute("LOGIN_USER_ROLE");
        String role = roleObj != null ? roleObj.toString() : "";

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "접근 권한이 없습니다.", null));
        }

        // 3. 비즈니스 로직 실행
        WordResponseDto data = wordService.createWord(request);

        // 4. 명세서 201 Created 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "단어가 추가되었습니다.", data));
    }
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<WordResponseDto>> updateWord(
            @PathVariable Long id,
            @Valid @RequestBody WordUpdateRequestDto request,
            HttpServletRequest httpRequest) {

        // 1. 명세서 400 에러: 아무것도 변경하지 않는 빈 요청 필터링
        if (request.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, "수정할 항목을 최소 1개 이상 입력해주세요.", null));
        }

        // 2. 명세서 401 에러: 인증(로그인) 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 3. 명세서 403 에러: ADMIN 권한 확인 (현우님과 상의 전까지 임시 주석 처리 가능)
        Object roleObj = session.getAttribute("LOGIN_USER_ROLE");
        String role = roleObj != null ? roleObj.toString() : "";

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "접근 권한이 없습니다.", null));
        }

        // 4. 비즈니스 로직 실행
        WordResponseDto data = wordService.updateWord(id, request);

        // 5. 명세서 200 OK 응답 반환
        return ResponseEntity.ok(new ApiResponse<>(true, "단어가 수정되었습니다.", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWord(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        // 1. 명세서 401 에러: 인증(로그인) 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 2. 명세서 403 에러: ADMIN 권한 확인 (테스트 중이라면 임시 주석 처리!)
        Object roleObj = session.getAttribute("LOGIN_USER_ROLE");
        String role = roleObj != null ? roleObj.toString() : "";

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "접근 권한이 없습니다.", null));
        }

        // 3. 비즈니스 로직(삭제) 실행
        wordService.deleteWord(id);

        // 4. 명세서 200 OK 응답 반환
        return ResponseEntity.ok(new ApiResponse<>(true, "단어가 삭제되었습니다.", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<WordListResponseDto>> getWordList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort,
            HttpServletRequest httpRequest) {

        // 1. 명세서 401 에러: 인증(로그인) 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 2. 세션에서 현재 로그인한 유저 ID 꺼내기 (추후 isMemorized 조회용)
        Object userIdObj = session.getAttribute("LOGIN_USER_ID");
        Long userId = Long.valueOf(userIdObj.toString());

        // 3. 비즈니스 로직 실행 (권한 검사 생략 - USER, ADMIN 모두 가능)
        WordListResponseDto data = wordService.getWordList(page, size, sort, userId);

        // 4. 명세서 200 OK 응답 반환
        return ResponseEntity.ok(new ApiResponse<>(true, "단어 목록 조회 성공", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WordDetailResponseDto>> getWordDetail(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        // 1. 명세서 401 에러: 인증(로그인) 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 2. 세션에서 로그인한 유저 ID 꺼내기
        Object userIdObj = session.getAttribute("LOGIN_USER_ID");
        Long userId = Long.valueOf(userIdObj.toString());

        // 3. 비즈니스 로직 실행
        WordDetailResponseDto data = wordService.getWordDetail(id, userId);

        // 4. 명세서 200 OK 응답 반환
        return ResponseEntity.ok(new ApiResponse<>(true, "단어 상세 조회 성공", data));
    }

}