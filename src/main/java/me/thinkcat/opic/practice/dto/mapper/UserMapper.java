package me.thinkcat.opic.practice.dto.mapper;

import me.thinkcat.opic.practice.dto.response.UserResponse;
import me.thinkcat.opic.practice.entity.User;

public class UserMapper {

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
