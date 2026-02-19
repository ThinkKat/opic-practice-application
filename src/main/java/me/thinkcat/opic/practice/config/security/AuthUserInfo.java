package me.thinkcat.opic.practice.config.security;

import lombok.Builder;
import lombok.Getter;
import me.thinkcat.opic.practice.entity.UserRole;

@Getter
@Builder
public class AuthUserInfo {

    private final Long userId;
    private final String username;
    private final UserRole role;
}
