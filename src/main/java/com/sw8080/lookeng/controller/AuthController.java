package com.sw8080.lookeng.controller;

import com.sw8080.lookeng.ApiResponse;
import com.sw8080.lookeng.dto.request.LoginRequestDto;
import com.sw8080.lookeng.dto.request.SignupRequestDto;
import com.sw8080.lookeng.dto.response.CommonResponse;
import com.sw8080.lookeng.dto.response.LoginResponseDto;
import com.sw8080.lookeng.dto.response.SignupResponseDto;
import com.sw8080.lookeng.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponseDto>> signup(
            @Valid @RequestBody SignupRequestDto request) {

        SignupResponseDto response = authService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "회원가입이 완료되었습니다.", response));
    }
    @PostMapping("/admin/signup")
    public ResponseEntity<ApiResponse<SignupResponseDto>> adminSignup(
            @Valid @RequestBody SignupRequestDto request) {

        // 관리자 가입 서비스 호출
        SignupResponseDto response = authService.adminSignup(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "관리자 회원가입이 완료되었습니다.", response));
    }
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {

        // 1. 로그인 로직 실행
        LoginResponseDto response = authService.login(request);

        // 2. 세션 생성 및 유저 식별자 저장 (명세서 요구사항: JSESSIONID 발급)
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("LOGIN_USER_ID", response.getUserId());
        session.setAttribute("LOGIN_USER_ROLE", response.getRole());
        session.setMaxInactiveInterval(1800); // 세션 만료 30분 설정 (선택사항)


        // 3. 200 OK 응답 반환
        return ResponseEntity.ok(new CommonResponse<>(true, "로그인 성공", response));
    }
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(HttpServletRequest request) {
        // 1. 현재 사용자의 세션을 가져옵니다. (false: 세션이 없으면 새로 만들지 않고 null을 반환)
        HttpSession session = request.getSession(false);

        // 2. 명세서 에러 응답(401): 세션이 없거나 이미 로그아웃된 상태라면 에러 발생
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            // 우리가 GlobalExceptionHandler에 설정해둔 401 에러를 발생시킵니다.
            throw new IllegalArgumentException("세션이 없습니다. (미로그인 상태)");
        }

        // 3. 세션 무효화 (출입증 폐기)
        session.invalidate();

        // 4. 명세서 200 OK 응답 반환 (data는 null)
        return ResponseEntity.ok(new CommonResponse<>(true, "로그아웃 성공", null));
    }
}
