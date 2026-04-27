package com.example.allinmarket.common.security;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    static final int MAX_FAILURES = 10;
    static final long BLOCK_DURATION_SECONDS = 300;
    static final String KEY_PREFIX = "login:fail:";

    private static final Set<String> LOGIN_PATHS = Set.of("/auth/login", "/seller/auth/login");

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !LOGIN_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = extractClientIp(request);
        String key = KEY_PREFIX + ip;

        String countStr = stringRedisTemplate.opsForValue().get(key);
        if (countStr != null && Integer.parseInt(countStr) >= MAX_FAILURES) {
            log.warn("[RateLimit] 로그인 시도 횟수 초과 - IP: {}", ip);
            sendRateLimitError(response);
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, responseWrapper);

        int status = responseWrapper.getStatus();
        if (status == 400) {
            incrementFailureCount(key, ip);
        } else if (status < 300) {
            stringRedisTemplate.delete(key);
        }

        responseWrapper.copyBodyToResponse();
    }

    private void incrementFailureCount(String key, String ip) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, BLOCK_DURATION_SECONDS, TimeUnit.SECONDS);
        }
        log.warn("[RateLimit] 로그인 실패 - IP: {}, 누적 횟수: {}/{}", ip, count, MAX_FAILURES);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void sendRateLimitError(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(ErrorEnum.LOGIN_RATE_LIMITED)
        ));
    }
}
