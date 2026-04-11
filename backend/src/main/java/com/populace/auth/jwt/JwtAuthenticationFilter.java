package com.populace.auth.jwt;

import com.populace.auth.service.UserPrincipal;
import com.populace.platform.repository.PlatformAdminRepository;
import com.populace.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;

    public JwtAuthenticationFilter(
            JwtTokenProvider tokenProvider,
            UserRepository userRepository,
            PlatformAdminRepository platformAdminRepository) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.platformAdminRepository = platformAdminRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            String tokenType = tokenProvider.getTokenType(token);
            Long subjectId = tokenProvider.getUserIdFromToken(token);

            switch (tokenType) {
                case "platform_admin" -> platformAdminRepository.findById(subjectId).ifPresent(admin -> {
                    UserPrincipal principal = UserPrincipal.fromPlatformAdmin(admin);
                    setAuthentication(principal, request);
                });
                case "impersonation" -> {
                    Long businessId = tokenProvider.getBusinessIdFromToken(token);
                    platformAdminRepository.findById(subjectId).ifPresent(admin -> {
                        UserPrincipal principal = UserPrincipal.forImpersonation(admin, businessId);
                        setAuthentication(principal, request);
                    });
                }
                default -> userRepository.findById(subjectId).ifPresent(user -> {
                    UserPrincipal principal = UserPrincipal.from(user);
                    setAuthentication(principal, request);
                });
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(UserPrincipal principal, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        authentication.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
