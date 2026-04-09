package com.example.allinmarket.common.security;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
//    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            // Redis 블랙리스트 확인 — 장애 시 fail-closed (503 반환)
//            try {
//                if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
//                    sendError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorEnum.TOKEN_INVALID);
//                    return;
//                }
//            } catch (Exception e) {
//                log.error("[JWT] Redis 연결 오류 - fail-closed 처리", e);
//                sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, ErrorEnum.INTERNAL_SERVER_ERROR);
//                return;
//            }

            if (!jwtProvider.validateToken(token)) {
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorEnum.TOKEN_INVALID);
                return;
            }

            Long userId = jwtProvider.getUserId(token);
            UserRole role = jwtProvider.getRole(token);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private void sendError(HttpServletResponse response, int status, ErrorEnum errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"data\":null,\"message\":\"" + errorCode.getMessage() + "\"}"
        );
    }
}

