package me.thinkcat.opic.practice.config.webconfig;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.thinkcat.opic.practice.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalApiKeyInterceptor implements HandlerInterceptor {

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String apiKey = request.getHeader("X-Internal-Api-Key");
        if (!internalApiKey.equals(apiKey)) {
            throw new UnauthorizedException("Invalid or missing API key");
        }
        return true;
    }
}
