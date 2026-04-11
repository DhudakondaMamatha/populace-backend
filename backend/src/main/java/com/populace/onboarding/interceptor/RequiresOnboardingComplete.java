package com.populace.onboarding.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark controllers or methods that require onboarding to be complete.
 * When applied, the OnboardingInterceptor will verify the business has completed
 * all required onboarding stages before allowing access.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresOnboardingComplete {

    /**
     * Custom error message when onboarding is incomplete.
     */
    String message() default "Please complete business setup first";
}
