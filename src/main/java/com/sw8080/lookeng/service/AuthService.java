package com.sw8080.lookeng.service;

import com.sw8080.lookeng.DuplicateEmailException;
import com.sw8080.lookeng.Role;
import com.sw8080.lookeng.dto.request.SignupRequestDto;
import com.sw8080.lookeng.dto.response.SignupResponseDto;
import com.sw8080.lookeng.entity.User;
import com.sw8080.lookeng.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponseDto signup(SignupRequestDto request) {
        // 1. 중복 이메일 검증
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
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
}
