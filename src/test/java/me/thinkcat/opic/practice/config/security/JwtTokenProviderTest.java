package me.thinkcat.opic.practice.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", "testsecretkeyfortestingpurposesonly12345678");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValidity", 3600000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenValidity", 1209600000L);
        jwtTokenProvider.init();
    }

    @Test
    void 동일_사용자_동일_시점에_generateRefreshToken_연속_호출시_서로다른_토큰_반환() {
        String token1 = jwtTokenProvider.generateRefreshToken("user", 1L);
        String token2 = jwtTokenProvider.generateRefreshToken("user", 1L);

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void generateRefreshToken_100회_호출시_모두_고유한_토큰_반환() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            tokens.add(jwtTokenProvider.generateRefreshToken("user", 1L));
        }

        assertThat(tokens).hasSize(100);
    }
}
