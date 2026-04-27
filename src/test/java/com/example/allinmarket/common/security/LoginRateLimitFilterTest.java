package com.example.allinmarket.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitFilterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private FilterChain filterChain;

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        filter = new LoginRateLimitFilter(stringRedisTemplate, objectMapper);
    }

    @Test
    void 실패_횟수가_미달이면_요청을_통과시킨다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("login:fail:1.2.3.4")).willReturn("3");
        given(valueOps.increment("login:fail:1.2.3.4")).willReturn(4L);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        verify(valueOps).increment("login:fail:1.2.3.4");
    }

    @Test
    void 실패_횟수가_임계값_이상이면_429를_반환한다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("login:fail:1.2.3.4")).willReturn(String.valueOf(LoginRateLimitFilter.MAX_FAILURES));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void 로그인_성공시_카운터를_삭제한다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("login:fail:1.2.3.4")).willReturn(null);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(200); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(stringRedisTemplate).delete("login:fail:1.2.3.4");
    }

    @Test
    void 첫번째_실패시_TTL을_설정한다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("login:fail:1.2.3.4")).willReturn(null);
        given(valueOps.increment("login:fail:1.2.3.4")).willReturn(1L);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(stringRedisTemplate).expire(
                eq("login:fail:1.2.3.4"),
                eq(LoginRateLimitFilter.BLOCK_DURATION_SECONDS),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void X_Forwarded_For_헤더의_마지막_IP를_사용한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("login:fail:192.168.1.1")).willReturn(null);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(200); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(stringRedisTemplate).delete("login:fail:192.168.1.1");
    }

    @Test
    void 로그인_경로_외의_요청은_필터를_건너뛴다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/signup");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void GET_요청은_필터를_건너뛴다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/login");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void 판매자_로그인_경로에도_필터가_적용된다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/seller/auth/login");

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    private MockHttpServletRequest loginRequest(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
