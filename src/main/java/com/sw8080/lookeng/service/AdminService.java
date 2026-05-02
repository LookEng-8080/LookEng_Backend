package com.sw8080.lookeng.service;

import com.sw8080.lookeng.Role;
import com.sw8080.lookeng.dto.response.UserListResponseDto;
import com.sw8080.lookeng.entity.User;
import com.sw8080.lookeng.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserListResponseDto getUsers() {
        // 1. USER 롤만 전체 조회 (최신 가입순)
        List<User> users = userRepository.findAllByRole(Role.USER);
        // 2. 응답 DTO 변환
        return UserListResponseDto.from(users);
    }
}
