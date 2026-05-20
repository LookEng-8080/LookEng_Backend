package com.sw8080.lookeng.controller;

import com.sw8080.lookeng.ApiResponse;
import com.sw8080.lookeng.Role;
import com.sw8080.lookeng.dto.request.*;
import com.sw8080.lookeng.dto.response.*;
import com.sw8080.lookeng.exception.ForbiddenException;
import com.sw8080.lookeng.exception.UnauthorizedException;
import com.sw8080.lookeng.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


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
    public ResponseEntity<ApiResponse<AdminSignupResponseDto>> adminSignup(
            @Valid @RequestBody AdminSignupRequestDto request) {

        // 관리자 가입 서비스 호출
        AdminSignupResponseDto response = authService.adminSignup(request);

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
            throw new UnauthorizedException("세션이 없습니다. (미로그인 상태)");
        }

        // 3. 세션 무효화 (출입증 폐기)
        session.invalidate();

        // 4. 명세서 200 OK 응답 반환 (data는 null)
        return ResponseEntity.ok(new CommonResponse<>(true, "로그아웃 성공", null));
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<CommonResponse<Void>> withdraw(HttpServletRequest request) {
        // 1. 세션 확인 및 유저 식별자 추출 (미로그인 시 401 예외)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            throw new UnauthorizedException("세션이 없습니다. (미로그인 상태)");
        }

        // 💡 2. 관리자 탈퇴 방지 1차 방어 (세션 권한 확인 시 403 예외)
        String roleString = (String) session.getAttribute("LOGIN_USER_ROLE");
        if (Role.ADMIN.name().equals(roleString)) {
            throw new ForbiddenException("관리자 계정은 탈퇴할 수 없습니다.");
        }

        // 3. 탈퇴 비즈니스 로직 실행
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        authService.withdraw(userId);

        // 4. 탈퇴 성공 후 세션 무효화 (출입증 즉시 폐기)
        session.invalidate();

        // 5. 명세서에 맞춘 200 OK 응답
        return ResponseEntity.ok(
                new CommonResponse<>(true, "회원 탈퇴 및 데이터 삭제가 정상적으로 완료되었습니다.", null)
        );
    }

    @PostMapping("/password/reset-request")
    public ResponseEntity<CommonResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto request) {

        authService.requestPasswordReset(request);

        return ResponseEntity.ok(
                new CommonResponse<>(true, "비밀번호 재설정 인증번호가 이메일로 발송되었습니다.", null)
        );
    }

    @PostMapping("/password/reset")
    public ResponseEntity<CommonResponse<Void>> resetPassword(
            @Valid @RequestBody com.sw8080.lookeng.dto.request.PasswordResetConfirmRequestDto request) {

        // 1. 비밀번호 변경 로직 실행
        authService.resetPassword(request);

        // 2. 성공 응답 반환
        return ResponseEntity.ok(
                new CommonResponse<>(true, "비밀번호가 성공적으로 변경되었습니다.", null)
        );
    }

    @PostMapping("/social")
    public ResponseEntity<CommonResponse<SocialLoginResponseDto>> socialLogin(
            @Valid @RequestBody SocialLoginRequestDto request,
            HttpServletRequest httpRequest) {

        // 1. 소셜 로그인 로직 실행
        SocialLoginResponseDto response = authService.socialLogin(request);

        // 2. 세션 생성 및 유저 식별자 저장
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("LOGIN_USER_ID", response.getUserId());
        session.setAttribute("LOGIN_USER_ROLE", response.getRole());
        session.setMaxInactiveInterval(1800); // 세션 만료 30분

        // 3. 200 OK 응답 반환
        return ResponseEntity.ok(new CommonResponse<>(true, "구글 소셜 로그인 성공", response));
    }

}
