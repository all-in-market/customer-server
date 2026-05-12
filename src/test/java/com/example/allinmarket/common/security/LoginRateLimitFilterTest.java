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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitFilterTest {

    private static final String EMAIL = "test@example.com";
    private static final String EMAIL_HASH = LoginRateLimitFilter.sha256(EMAIL);
    private static final String KEY_IP = LoginRateLimitFilter.KEY_PREFIX_IP + "1.2.3.4";
    private static final String KEY_IP_EMAIL = LoginRateLimitFilter.KEY_PREFIX_IP_EMAIL + "1.2.3.4:" + EMAIL_HASH;

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private FilterChain filterChain;

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter(stringRedisTemplate, new ObjectMapper().findAndRegisterModules());
    }

    // ── IP 글로벌 차단 ──────────────────────────────────────────────────────────

    @Test
    void IP_글로벌_제한_초과시_429를_반환한다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(KEY_IP)).willReturn(String.valueOf(LoginRateLimitFilter.MAX_FAILURES_IP));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
    }

    // ── IP+이메일 차단 ──────────────────────────────────────────────────────────

    @Test
    void IP_이메일_제한_초과시_429를_반환한다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(KEY_IP)).willReturn("3");
        given(valueOps.get(KEY_IP_EMAIL)).willReturn(String.valueOf(LoginRateLimitFilter.MAX_FAILURES_IP_EMAIL));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
    }

    // ── 실패 시 카운터 증가 ──────────────────────────────────────────────────────

    @Test
    void 로그인_실패시_IP_카운터를_증가시킨다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(valueOps).increment(KEY_IP);
    }

    @Test
    void 로그인_실패시_IP_이메일_카운터를_증가시킨다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(valueOps).increment(KEY_IP_EMAIL);
    }

    @Test
    void 첫번째_실패시_IP_키에_TTL을_설정한다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.increment(KEY_IP)).willReturn(1L);
        given(valueOps.increment(KEY_IP_EMAIL)).willReturn(2L); // 2L → IP+이메일 키 TTL 미설정
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(stringRedisTemplate).expire(
                eq(KEY_IP), eq(LoginRateLimitFilter.BLOCK_DURATION_IP_SECONDS), eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void 첫번째_실패시_IP_이메일_키에_TTL을_설정한다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.increment(KEY_IP)).willReturn(2L); // 2L → IP 키 TTL 미설정
        given(valueOps.increment(KEY_IP_EMAIL)).willReturn(1L);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(stringRedisTemplate).expire(
                eq(KEY_IP_EMAIL), eq(LoginRateLimitFilter.BLOCK_DURATION_IP_EMAIL_SECONDS), eq(TimeUnit.SECONDS)
        );
    }

    // ── 성공 시 카운터 처리 ──────────────────────────────────────────────────────

    @Test
    void 로그인_성공시_IP_이메일_카운터를_삭제한다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(200); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(stringRedisTemplate).delete(KEY_IP_EMAIL);
    }

    @Test
    void 로그인_성공시_IP_카운터는_삭제하지_않는다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(200); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(stringRedisTemplate, never()).delete(KEY_IP);
    }

    // ── 이메일 처리 ─────────────────────────────────────────────────────────────

    @Test
    void 이메일이_없으면_IP_카운터만_증가시킨다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("1.2.3.4");
        request.setContentType("application/json");
        request.setContent("{\"password\":\"pass1234\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(valueOps).increment(KEY_IP);
        verify(valueOps, never()).increment(KEY_IP_EMAIL);
    }

    @Test
    void 이메일_대소문자는_소문자로_정규화된다() throws Exception {
        String upperEmail = "Test@Example.COM";
        // "test@example.com"의 해시와 동일해야 한다
        String expectedKey = LoginRateLimitFilter.KEY_PREFIX_IP_EMAIL + "1.2.3.4:" + LoginRateLimitFilter.sha256("test@example.com");

        MockHttpServletRequest request = loginRequest("1.2.3.4", upperEmail);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(valueOps).increment(expectedKey);
    }

    @Test
    void 빈_이메일은_IP_단독_제한으로_처리한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("1.2.3.4");
        request.setContentType("application/json");
        request.setContent("{\"email\":\"   \",\"password\":\"pass1234\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(valueOps).increment(KEY_IP);
        verify(valueOps, never()).increment(KEY_IP_EMAIL);
    }

    // ── IP 추출 ─────────────────────────────────────────────────────────────────

    @Test
    void X_Forwarded_For_헤더의_마지막_IP를_사용한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
        setJsonBody(request, EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        String expectedIpEmailKey = LoginRateLimitFilter.KEY_PREFIX_IP_EMAIL + "192.168.1.1:" + EMAIL_HASH;
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(200); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(stringRedisTemplate).delete(expectedIpEmailKey);
    }

    // ── shouldNotFilter ─────────────────────────────────────────────────────────

    @Test
    void 로그인_경로_외의_요청은_필터를_건너뛴다() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/auth/signup"))).isTrue();
    }

    @Test
    void GET_요청은_필터를_건너뛴다() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/auth/login"))).isTrue();
    }

    @Test
    void 판매자_로그인_경로에도_필터가_적용된다() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/seller/auth/login"))).isFalse();
    }

    // ── 헬퍼 ───────────────────────────────────────────────────────────────────

    private MockHttpServletRequest loginRequest(String remoteAddr, String email) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr(remoteAddr);
        setJsonBody(request, email);
        return request;
    }

    private void setJsonBody(MockHttpServletRequest request, String email) {
        request.setContentType("application/json");
        request.setContent(
                ("{\"email\":\"" + email + "\",\"password\":\"pass1234\"}").getBytes(StandardCharsets.UTF_8)
        );
    }
}
