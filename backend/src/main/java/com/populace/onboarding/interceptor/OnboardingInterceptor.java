package com.populace.onboarding.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.populace.auth.util.SecurityUtils;
import com.populace.onboarding.service.OnboardingEvaluationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;

@Component
public class OnboardingInterceptor implements HandlerInterceptor {

    private final OnboardingEvaluationService evaluationService;
    private final ObjectMapper objectMapper;

    public OnboardingInterceptor(OnboardingEvaluationService evaluationService,
                                  ObjectMapper objectMapper) {
        this.evaluationService = evaluationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!isHandlerMethod(handler)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequiresOnboardingComplete annotation = findAnnotation(handlerMethod);

        if (annotation == null) {
            return true;
        }

        Long businessId = SecurityUtils.getCurrentBusinessId();

        if (businessId == null) {
            return true;
        }

        boolean isComplete = evaluationService.isOnboardingComplete(businessId);

        if (!isComplete) {
            writeErrorResponse(response, annotation.message());
            return false;
        }

        return true;
    }

    private boolean isHandlerMethod(Object handler) {
        return handler instanceof HandlerMethod;
    }

    private RequiresOnboardingComplete findAnnotation(HandlerMethod handlerMethod) {
        RequiresOnboardingComplete methodAnnotation =
            handlerMethod.getMethodAnnotation(RequiresOnboardingComplete.class);

        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        return handlerMethod.getBeanType().getAnnotation(RequiresOnboardingComplete.class);
    }

    private void writeErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> errorBody = Map.of(
            "error", "ONBOARDING_INCOMPLETE",
            "message", message
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
