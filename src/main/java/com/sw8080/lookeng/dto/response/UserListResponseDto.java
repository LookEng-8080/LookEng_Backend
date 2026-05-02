package com.sw8080.lookeng.dto.response;

import com.sw8080.lookeng.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserListResponseDto {

    private List<UserItemDto> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;

    public static UserListResponseDto from(Page<User> page) {
        return UserListResponseDto.builder()
                .content(page.getContent().stream().map(UserItemDto::from).toList())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .size(page.getSize())
                .build();
    }
}
