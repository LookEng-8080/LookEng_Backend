package com.sw8080.lookeng.service;

import com.sw8080.lookeng.Role;
import com.sw8080.lookeng.dto.request.LoginRequestDto;
import com.sw8080.lookeng.dto.request.SignupRequestDto;
import com.sw8080.lookeng.dto.response.LoginResponseDto;
import com.sw8080.lookeng.dto.response.SignupResponseDto;
import com.sw8080.lookeng.entity.User;
import com.sw8080.lookeng.exception.DuplicateException;
import com.sw8080.lookeng.exception.UnauthorizedException;
import com.sw8080.lookeng.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공 - USER 역할 부여")
    void signup_success() {
        SignupRequestDto request = new SignupRequestDto("test@test.com", "password123", "테스터");
        User savedUser = User.builder()
                .email("test@test.com")
                .passwordHash("encoded")
                .nickname("테스터")
                .role(Role.USER)
                .build();

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        SignupResponseDto result = authService.signup(request);

        assertThat(result.getEmail()).isEqualTo("test@test.com");
        assertThat(result.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이메일 → DuplicateException")
    void signup_duplicateEmail() {
        SignupRequestDto request = new SignupRequestDto("dup@test.com", "password123", "테스터");
        given(userRepository.existsByEmail(anyString())).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateException.class);
    }

    @Test
    @DisplayName("관리자 회원가입 성공 - ADMIN 역할 부여")
    void adminSignup_success() {
        SignupRequestDto request = new SignupRequestDto("admin@test.com", "password123", "관리자");
        User savedAdmin = User.builder()
                .email("admin@test.com")
                .passwordHash("encoded")
                .nickname("관리자")
                .role(Role.ADMIN)
                .build();

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any(User.class))).willReturn(savedAdmin);

        SignupResponseDto result = authService.adminSignup(request);

        assertThat(result.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("로그인 성공 - 사용자 정보 반환")
    void login_success() {
        LoginRequestDto request = new LoginRequestDto("test@test.com", "password123");
        User user = User.builder()
                .email("test@test.com")
                .passwordHash("encoded")
                .nickname("테스터")
                .role(Role.USER)
                .build();

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

        LoginResponseDto result = authService.login(request);

        assertThat(result.getEmail()).isEqualTo("test@test.com");
        assertThat(result.getNickname()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일 → UnauthorizedException (500 아님)")
    void login_emailNotFound_shouldReturn401NotThrow500() {
        LoginRequestDto request = new LoginRequestDto("none@test.com", "password123");
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치 → UnauthorizedException")
    void login_wrongPassword() {
        LoginRequestDto request = new LoginRequestDto("test@test.com", "wrongpassword");
        User user = User.builder()
                .email("test@test.com")
                .passwordHash("encoded")
                .nickname("테스터")
                .role(Role.USER)
                .build();

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }
}
