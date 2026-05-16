package com.sw8080.lookeng.service;

import com.sw8080.lookeng.Role;
import com.sw8080.lookeng.dto.request.AdminSignupRequestDto;
import com.sw8080.lookeng.dto.request.PasswordResetRequestDto;
import com.sw8080.lookeng.dto.response.AdminSignupResponseDto;
import com.sw8080.lookeng.entity.PasswordResetToken;
import com.sw8080.lookeng.exception.DuplicateException;
import com.sw8080.lookeng.exception.NotFoundException;
import com.sw8080.lookeng.exception.UnauthorizedException;
import com.sw8080.lookeng.dto.request.LoginRequestDto;
import com.sw8080.lookeng.dto.request.SignupRequestDto;
import com.sw8080.lookeng.dto.response.LoginResponseDto;
import com.sw8080.lookeng.dto.response.SignupResponseDto;
import com.sw8080.lookeng.entity.User;
import com.sw8080.lookeng.repository.PasswordResetTokenRepository;
import com.sw8080.lookeng.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.sw8080.lookeng.exception.ForbiddenException;
import org.springframework.mail.SimpleMailMessage;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${lookeng.admin.secret-code}")
    private String adminSecretCode;

    @Transactional
    public SignupResponseDto signup(SignupRequestDto request) {
        // 1. 중복 이메일 검증
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateException("이미 사용 중인 이메일입니다.");
        }

        // 2. 엔티티 생성 (ERD 반영: password_hash 필드 사용)
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        // 3. 응답 DTO 변환
        return SignupResponseDto.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        // 1. 이메일로 유저 찾기 (엔티티의 @SQLRestriction 덕분에 탈퇴한 회원은 자동 제외됨)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 일치하지 않습니다."));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // 보안을 위해 이메일이 틀린 것인지 비번이 틀린 것인지 알 수 없게 동일한 메시지 반환
            throw new UnauthorizedException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // 3. 명세서에 맞춘 응답 DTO 반환
        return LoginResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public AdminSignupResponseDto adminSignup(AdminSignupRequestDto request) {

        // 💡 1. 관리자 코드 일치 여부 검증
        if (!adminSecretCode.equals(request.getAdminCode())) {
            throw new UnauthorizedException("관리자 코드가 일치하지 않습니다.");
        }

        // 2. 중복 이메일 검증
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateException("이미 사용 중인 이메일입니다.");
        }

        // 3. 관리자 엔티티 생성 (passwordHash 필드 유지)
        User admin = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(Role.ADMIN)
                .build();

        User savedAdmin = userRepository.save(admin);

        // 4. 응답 DTO 변환 및 반환
        return AdminSignupResponseDto.builder()
                .id(savedAdmin.getId())
                .email(savedAdmin.getEmail())
                .nickname(savedAdmin.getNickname())
                .role(savedAdmin.getRole().name())
                .build();
    }


    @Transactional
    public void withdraw(Long userId) {
        // 1. 세션에서 가져온 ID로 유저 조회 (없으면 404 예외)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("해당 사용자를 찾을 수 없습니다."));

        // 💡 2. 관리자 탈퇴 방지 2차 방어 (DB 상의 진짜 권한 확인)
        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("관리자 계정은 탈퇴할 수 없습니다.");
        }

        // 3. 일반 사용자일 경우에만 DB에서 레코드 완전 삭제 (하드 딜리트)
        userRepository.delete(user);


    }
    @Transactional
    public void requestPasswordReset(PasswordResetRequestDto request) {
        // 1. 유저 조회 (없으면 404 예외)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("해당 이메일로 가입된 계정을 찾을 수 없습니다."));

        // 2. 소셜 로그인 유저 검증 (비밀번호가 없으면 403 예외)
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            throw new ForbiddenException("소셜 로그인으로 가입한 계정은 비밀번호를 재설정할 수 없습니다.");
        }

        // 3. 토큰 생성 및 저장 (유효시간 30분)
        int token = (int)(Math.random() * 90000) + 10000;
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiredAt(LocalDateTime.now().plusMinutes(3))
                .build();

        passwordResetTokenRepository.save(resetToken);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("[LookEng] 비밀번호 재설정 인증번호 안내"); // 제목 변경
        message.setText("안녕하세요. LookEng 비밀번호 재설정 안내 메일입니다.\n\n" +
                "아래 5자리 인증번호를 진행 중인 화면에 입력해 주세요.\n" +
                "인증번호 유효시간은 3분입니다.\n\n" +
                "인증번호 : [" + token + "]\n\n" +  // 💡 링크 대신 토큰 숫자만 띡!
                "감사합니다.");

        mailSender.send(message);
    }

    @Transactional
    public void resetPassword(com.sw8080.lookeng.dto.request.PasswordResetConfirmRequestDto request) {
        // 1. 유저 확인
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("해당 이메일로 가입된 계정을 찾을 수 없습니다."));

        // 2. 토큰 확인
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new NotFoundException("유효하지 않은 인증 번호입니다."));

        // 3. 보안 검증 (본인 토큰 맞는지, 만료 안 됐는지, 안 쓴 건지)
        if (!resetToken.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("본인의 인증 번호만 사용할 수 있습니다.");
        }
        if (resetToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("인증 번호의 유효시간(3분)이 만료되었습니다.");
        }
        if (resetToken.getUsedAt() != null) {
            throw new UnauthorizedException("이미 사용된 인증 번호입니다.");
        }

        // 4. 새 비밀번호 암호화 및 DB 업데이트
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));

        // 5. 토큰 사용 완료 처리 (다시 못 쓰게)
        resetToken.useToken();
    }
}
