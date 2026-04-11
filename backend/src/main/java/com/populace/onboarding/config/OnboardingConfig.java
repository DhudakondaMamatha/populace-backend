package com.populace.onboarding.config;

import com.populace.onboarding.interceptor.OnboardingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OnboardingConfig implements WebMvcConfigurer {

    private final OnboardingInterceptor onboardingInterceptor;

    public OnboardingConfig(OnboardingInterceptor onboardingInterceptor) {
        this.onboardingInterceptor = onboardingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(onboardingInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/auth/**",
                "/api/signup/**",
                "/api/onboarding/**",
                "/api/navigation/**",
                "/api/platform/**"
            );
    }
}
