package com.sw8080.lookeng.controller;

import com.sw8080.lookeng.ApiResponse;
import com.sw8080.lookeng.dto.request.WordCreateRequestDto;
import com.sw8080.lookeng.dto.response.BulkUploadResponseDto;
import com.sw8080.lookeng.dto.response.WordDetailResponseDto;
import com.sw8080.lookeng.dto.response.WordListResponseDto;
import com.sw8080.lookeng.dto.response.WordResponseDto;
import com.sw8080.lookeng.dto.response.WordSearchResponseDto;
import com.sw8080.lookeng.dto.response.WordUpdateRequestDto;
import com.sw8080.lookeng.service.WordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    @PostMapping
    public ResponseEntity<ApiResponse<WordResponseDto>> createWord(
            @Valid @RequestBody WordCreateRequestDto request,
            HttpServletRequest httpRequest) {

        // 1. 인증 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 2. ADMIN 권한 확인
        Object roleObj = session.getAttribute("LOGIN_USER_ROLE");
        String role = roleObj != null ? roleObj.toString() : "";
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "관리자 접근 권한이 없습니다.", null));
        }

        // 3. 비즈니스 로직 실행
        WordResponseDto data = wordService.createWord(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "단어가 추가되었습니다.", data));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkUploadResponseDto>> bulkUpload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        // 1. 인증 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 2. ADMIN 권한 확인
        Object roleObj = session.getAttribute("LOGIN_USER_ROLE");
        String role = roleObj != null ? roleObj.toString() : "";
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "관리자 접근 권한이 없습니다.", null));
        }

        // 3. 파일 누락 확인
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, "파일을 선택해주세요.", null));
        }

        // 4. 비즈니스 로직 실행
        BulkUploadResponseDto data = wordService.bulkUpload(file);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "단어 일괄 추가가 완료되었습니다.", data));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<WordSearchResponseDto>> searchWords(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        // 1. 인증 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 2. keyword 누락 확인
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, "검색어를 입력해주세요.", null));
        }

        // 3. 세션에서 userId 추출
        Object userIdObj = session.getAttribute("LOGIN_USER_ID");
        Long userId = Long.valueOf(userIdObj.toString());

        // 4. 비즈니스 로직 실행 (USER / ADMIN 모두 가능)
        WordSearchResponseDto data = wordService.searchWords(keyword, page, size, userId);

        return ResponseEntity.ok(new ApiResponse<>(true, "단어 검색 결과를 성공적으로 조회했습니다.", data));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<WordResponseDto>> updateWord(
            @PathVariable Long id,
            @Valid @RequestBody WordUpdateRequestDto request,
            HttpServletRequest httpRequest) {

        // 1. 빈 요청 필터링
        if (request.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, "수정할 항목을 최소 1개 이상 입력해주세요.", null));
        }

        // 2. 인증 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 3. ADMIN 권한 확인
        Object roleObj = session.getAttribute("LOGIN_USER_ROLE");
        String role = roleObj != null ? roleObj.toString() : "";
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "관리자 접근 권한이 없습니다.", null));
        }

        // 4. 비즈니스 로직 실행
        WordResponseDto data = wordService.updateWord(id, request);

        return ResponseEntity.ok(new ApiResponse<>(true, "단어가 수정되었습니다.", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWord(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        // 1. 인증 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 2. ADMIN 권한 확인
        Object roleObj = session.getAttribute("LOGIN_USER_ROLE");
        String role = roleObj != null ? roleObj.toString() : "";
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "관리자 접근 권한이 없습니다.", null));
        }

        // 3. 비즈니스 로직 실행
        wordService.deleteWord(id);

        return ResponseEntity.ok(new ApiResponse<>(true, "단어가 삭제되었습니다.", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<WordListResponseDto>> getWordList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort,
            HttpServletRequest httpRequest) {

        // 1. 인증 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 2. userId 추출
        Object userIdObj = session.getAttribute("LOGIN_USER_ID");
        Long userId = Long.valueOf(userIdObj.toString());

        // 3. 비즈니스 로직 실행
        WordListResponseDto data = wordService.getWordList(page, size, sort, userId);

        return ResponseEntity.ok(new ApiResponse<>(true, "단어 목록 조회 성공", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WordDetailResponseDto>> getWordDetail(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        // 1. 인증 확인
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인이 필요한 서비스입니다.", null));
        }

        // 2. userId 추출
        Object userIdObj = session.getAttribute("LOGIN_USER_ID");
        Long userId = Long.valueOf(userIdObj.toString());

        // 3. 비즈니스 로직 실행
        WordDetailResponseDto data = wordService.getWordDetail(id, userId);

        return ResponseEntity.ok(new ApiResponse<>(true, "단어 상세 조회 성공", data));
    }
}
