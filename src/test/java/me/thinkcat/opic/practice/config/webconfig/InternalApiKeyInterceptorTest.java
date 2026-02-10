package me.thinkcat.opic.practice.config.webconfig;

import me.thinkcat.opic.practice.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalApiKeyInterceptorTest {

    private InternalApiKeyInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String VALID_API_KEY = "testtesttesttesttest";

    @BeforeEach
    void setUp() {
        interceptor = new InternalApiKeyInterceptor();
        ReflectionTestUtils.setField(interceptor, "internalApiKey", VALID_API_KEY);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void validApiKey_returnsTrue() {
        request.addHeader("X-Internal-Api-Key", VALID_API_KEY);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void invalidApiKey_throwsUnauthorizedException() {
        request.addHeader("X-Internal-Api-Key", "wrong-key");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void missingApiKey_throwsUnauthorizedException() {
        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(UnauthorizedException.class);
    }
}
