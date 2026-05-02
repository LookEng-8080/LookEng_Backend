package com.sw8080.lookeng.dto.response;

import com.sw8080.lookeng.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserListResponseDto {

    private List<UserItemDto> content;
    private long totalElements;

    public static UserListResponseDto from(List<User> users) {
        List<UserItemDto> content = users.stream().map(UserItemDto::from).toList();
        return UserListResponseDto.builder()
                .content(content)
                .totalElements(content.size())
                .build();
    }
}
