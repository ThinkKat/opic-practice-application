package me.thinkcat.opic.practice.security;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthUserInfo {

    private final Long userId;
    private final String username;
}
