package com.example.allinmarket.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
    private static final String KEY_IP_EMAIL = LoginRateLimitFilter.KEY_PREFIX_IP_EMAIL + "1.2.3.4:" + EMAIL_HASH;
    private static final String KEY_EMAIL = LoginRateLimitFilter.KEY_PREFIX_EMAIL + EMAIL_HASH;

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private FilterChain filterChain;

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter(stringRedisTemplate, new ObjectMapper().findAndRegisterModules());
    }

    // ── IP+이메일 차단 ──────────────────────────────────────────────────────────

    @Test
    void IP_이메일_제한_초과시_429를_반환한다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(KEY_IP_EMAIL)).willReturn(String.valueOf(LoginRateLimitFilter.MAX_FAILURES_IP_EMAIL));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
    }

    // ── 이메일 전용 차단 ────────────────────────────────────────────────────────

    @Test
    void 이메일_전용_제한_초과시_429를_반환한다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(KEY_IP_EMAIL)).willReturn("3");
        given(valueOps.get(KEY_EMAIL)).willReturn(String.valueOf(LoginRateLimitFilter.MAX_FAILURES_EMAIL));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
    }

    // ── 성공 시 두 카운터 초기화 ────────────────────────────────────────────────

    @Test
    void 로그인_성공시_두_카운터를_모두_삭제한다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(200); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(stringRedisTemplate).delete(KEY_IP_EMAIL);
        verify(stringRedisTemplate).delete(KEY_EMAIL);
    }

    // ── 실패 시 두 카운터 증가 ──────────────────────────────────────────────────

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
    void 로그인_실패시_이메일_카운터를_증가시킨다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(valueOps).increment(KEY_EMAIL);
    }

    @Test
    void 첫번째_실패시_IP_이메일_키에_TTL을_설정한다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.increment(KEY_IP_EMAIL)).willReturn(1L);
        given(valueOps.increment(KEY_EMAIL)).willReturn(2L);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(stringRedisTemplate).expire(
                eq(KEY_IP_EMAIL), eq(LoginRateLimitFilter.BLOCK_DURATION_SECONDS), eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void 첫번째_실패시_이메일_키에_TTL을_설정한다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.increment(KEY_IP_EMAIL)).willReturn(2L);
        given(valueOps.increment(KEY_EMAIL)).willReturn(1L);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(stringRedisTemplate).expire(
                eq(KEY_EMAIL), eq(LoginRateLimitFilter.BLOCK_DURATION_SECONDS), eq(TimeUnit.SECONDS)
        );
    }

    // ── 이메일 없을 때 폴백 ─────────────────────────────────────────────────────

    @Test
    void 잘못된_JSON이면_레이트_리밋_없이_요청을_통과시킨다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("1.2.3.4");
        request.setContentType("application/json");
        request.setContent("not-valid-json".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        // email = null → Redis 접근 없음 → 스텁 불필요

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void 빈_이메일이면_레이트_리밋_없이_요청을_통과시킨다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("1.2.3.4");
        request.setContentType("application/json");
        request.setContent("{\"email\":\"   \",\"password\":\"pass1234\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        // email.isBlank() → null → Redis 접근 없음

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        verify(stringRedisTemplate, never()).opsForValue();
    }

    // ── Redis 키 형식 검증 ─────────────────────────────────────────────────────

    @Test
    void Redis_키에는_이메일_원문이_아닌_해시를_사용한다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(valueOps).increment(KEY_IP_EMAIL);
        verify(valueOps).increment(KEY_EMAIL);
        assertThat(KEY_IP_EMAIL).doesNotContain(EMAIL).contains(EMAIL_HASH);
        assertThat(KEY_EMAIL).doesNotContain(EMAIL).contains(EMAIL_HASH);
    }

    // ── 요청 본문 재사용 ─────────────────────────────────────────────────────────

    @Test
    void 필터_실행_후에도_요청_본문을_읽을_수_있다() throws Exception {
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);

        AtomicReference<HttpServletRequest> captured = new AtomicReference<>();
        doAnswer(inv -> {
            captured.set((HttpServletRequest) inv.getArgument(0));
            ((HttpServletResponse) inv.getArgument(1)).setStatus(200);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        String body = new String(captured.get().getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(body).contains("\"email\":\"" + EMAIL + "\"");
    }

    // ── 이메일 정규화 ────────────────────────────────────────────────────────────

    @Test
    void 이메일_대소문자는_소문자로_정규화된다() throws Exception {
        // "Test@Example.COM" → sha256("test@example.com") = EMAIL_HASH
        MockHttpServletRequest request = loginRequest("1.2.3.4", "Test@Example.COM");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        // 대소문자 무관하게 동일한 해시 키를 사용한다
        verify(valueOps).increment(KEY_IP_EMAIL);
        verify(valueOps).increment(KEY_EMAIL);
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
        verify(stringRedisTemplate).delete(KEY_EMAIL);
    }

    // ── 요청 본문 크기 제한 ──────────────────────────────────────────────────────

    @Test
    void 요청_본문이_최대_크기를_초과하면_413을_반환한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("1.2.3.4");
        request.setContentType("application/json");
        byte[] oversizedBody = new byte[LoginRateLimitFilter.MAX_BODY_BYTES + 1];
        Arrays.fill(oversizedBody, (byte) 'a');
        request.setContent(oversizedBody);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(413);
    }

    // ── Redis Fail-open ─────────────────────────────────────────────────────────

    @Test
    void Redis_장애시_차단_확인이_fail_open으로_처리된다() throws Exception {
        // isBlocked에서 DataAccessException → false 반환 → 요청 통과
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(any())).willThrow(new RedisConnectionFailureException("Redis down"));
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(200); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    void Redis_장애시_카운터_증가가_fail_open으로_처리된다() throws Exception {
        // incrementFailureCount에서 DataAccessException → 로그 후 no-op
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.increment(any())).willThrow(new RedisConnectionFailureException("Redis down"));
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(400); return null; })
                .when(filterChain).doFilter(any(), any());

        // 예외가 전파되지 않으면 성공
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void Redis_장애시_카운터_삭제가_fail_open으로_처리된다() throws Exception {
        // deleteKey에서 DataAccessException → 로그 후 no-op
        MockHttpServletRequest request = loginRequest("1.2.3.4", EMAIL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(stringRedisTemplate.delete(any(String.class))).willThrow(new RedisConnectionFailureException("Redis down"));
        doAnswer(inv -> { ((HttpServletResponse) inv.getArgument(1)).setStatus(200); return null; })
                .when(filterChain).doFilter(any(), any());

        // 예외가 전파되지 않으면 성공
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
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
