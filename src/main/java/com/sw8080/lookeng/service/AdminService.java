package com.sw8080.lookeng.service;

import com.sw8080.lookeng.dto.response.UserListResponseDto;
import com.sw8080.lookeng.entity.User;
import com.sw8080.lookeng.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserListResponseDto getUsers(int page, int size) {
        // 1. 최신 가입순 페이지네이션
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // 2. 전체 유저 조회
        Page<User> users = userRepository.findAll(pageable);
        // 3. 응답 DTO 변환
        return UserListResponseDto.from(users);
    }
}
