package me.thinkcat.opic.practice.config.webconfig;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.config.security.AuthUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthUserArgumentResolver authUserArgumentResolver;
    private final InternalApiKeyInterceptor internalApiKeyInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authUserArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(internalApiKeyInterceptor)
                .addPathPatterns("/api/v1/answers/internal/**")
                .addPathPatterns("/api/v1/questions/internal/**")
                .addPathPatterns("/api/v1/drill-answers/internal/**");
    }
}
