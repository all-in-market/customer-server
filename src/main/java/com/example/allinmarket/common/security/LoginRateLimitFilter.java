package com.example.allinmarket.common.security;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    // IP+이메일: 같은 네트워크에서 특정 계정 반복 공격 방어 (5회 / 5분)
    static final int MAX_FAILURES_IP_EMAIL = 5;

    // 이메일 전용: 분산 IP를 사용한 특정 계정 공격 방어 (10회 / 5분)
    static final int MAX_FAILURES_EMAIL = 10;

    static final long BLOCK_DURATION_SECONDS = 300;

    static final String KEY_PREFIX_IP_EMAIL = "login:fail:ip-email:";
    static final String KEY_PREFIX_EMAIL = "login:fail:email:";

    private static final Set<String> LOGIN_PATHS = Set.of("/auth/login", "/seller/auth/login");

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private boolean isLoginRequest(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && LOGIN_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isLoginRequest(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyRequestWrapper requestWrapper = new CachedBodyRequestWrapper(request);
        String ip = extractClientIp(request);
        String email = extractEmail(requestWrapper.getBody());

        String ipEmailKey = (email != null) ? KEY_PREFIX_IP_EMAIL + ip + ":" + sha256(email) : null;
        String emailKey = (email != null) ? KEY_PREFIX_EMAIL + sha256(email) : null;

        if (ipEmailKey != null && isBlocked(ipEmailKey, MAX_FAILURES_IP_EMAIL)) {
            log.warn("[RateLimit] IP+이메일 차단 - IP: {}, email: {}", ip, email);
            sendRateLimitError(response);
            return;
        }

        if (emailKey != null && isBlocked(emailKey, MAX_FAILURES_EMAIL)) {
            log.warn("[RateLimit] 이메일 차단 - email: {}", email);
            sendRateLimitError(response);
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(requestWrapper, responseWrapper);

        int status = responseWrapper.getStatus();
        if (status == 400) {
            if (ipEmailKey != null) incrementFailureCount(ipEmailKey, MAX_FAILURES_IP_EMAIL);
            if (emailKey != null) incrementFailureCount(emailKey, MAX_FAILURES_EMAIL);
            log.warn("[RateLimit] 로그인 실패 - IP: {}, email: {}", ip, email);
        } else if (status < 300) {
            if (ipEmailKey != null) stringRedisTemplate.delete(ipEmailKey);
            if (emailKey != null) stringRedisTemplate.delete(emailKey);
        }

        responseWrapper.copyBodyToResponse();
    }

    private boolean isBlocked(String key, int maxFailures) {
        String countStr = stringRedisTemplate.opsForValue().get(key);
        if (countStr == null) return false;
        try {
            return Integer.parseInt(countStr) >= maxFailures;
        } catch (NumberFormatException e) {
            log.warn("[RateLimit] 실패 횟수 파싱 오류 - key: {}, value: '{}'", key, countStr);
            return false;
        }
    }

    private void incrementFailureCount(String key, int maxFailures) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, BLOCK_DURATION_SECONDS, TimeUnit.SECONDS);
        }
        log.warn("[RateLimit] 카운터 증가 - key: {}, 누적: {}/{}", key, count, maxFailures);
    }

    private String extractEmail(byte[] body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode emailNode = node.get("email");
            if (emailNode != null && emailNode.isTextual()) {
                String email = emailNode.asText().strip().toLowerCase();
                return email.isBlank() ? null : email;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] ips = xForwardedFor.split(",");
            return ips[ips.length - 1].trim();
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

    static String sha256(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        byte[] getBody() {
            return body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return bais.read(); }
                @Override public boolean isFinished() { return bais.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {}
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
