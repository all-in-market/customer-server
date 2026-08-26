package com.example.allinmarket.buyer.auth.service;

import com.example.allinmarket.buyer.auth.dto.request.BuyerLoginRequest;
import com.example.allinmarket.buyer.auth.dto.request.BuyerSignupRequest;
import com.example.allinmarket.buyer.auth.dto.response.BuyerAuthResponse;
import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.auth.dto.LoginResult;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.cart.entity.Cart;
import com.example.allinmarket.domain.cart.repository.CartRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BuyerAuthServiceTest {

    @Mock
    private BuyerRepository buyerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private BuyerAuthService buyerAuthService;

    @Nested
    @DisplayName("회원가입")
    class SignupTest {

        @Test
        @DisplayName("이메일이 중복되지 않으면 회원가입에 성공하고 장바구니가 함께 생성된다")
        void signup_success_whenEmailNotDuplicated() {
            // given
            BuyerSignupRequest request = new BuyerSignupRequest(
                    "테스트@테스트.com", "12345678", "테스트", "010-1234-1234");

            given(buyerRepository.existsByEmail("테스트@테스트.com")).willReturn(false);
            given(passwordEncoder.encode("12345678")).willReturn("암호화");

            Buyer savedBuyer = Buyer.of(request.email(), "암호화", request.name(), request.phone());
            given(buyerRepository.save(any(Buyer.class))).willReturn(savedBuyer);

            Cart savedCart = Cart.of(savedBuyer);
            given(cartRepository.save(any(Cart.class))).willReturn(savedCart);

            // when
            BuyerAuthResponse response = buyerAuthService.signup(request);

            // then
            assertThat(response.email()).isEqualTo("테스트@테스트.com");
            assertThat(response.name()).isEqualTo("테스트");
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("이메일이 중복되면 EMAIL_ALREADY_EXISTS를 던진다")
        void signup_fail_whenEmailAlreadyExists() {
            // given
            BuyerSignupRequest request = new BuyerSignupRequest(
                    "테스트@테스트.com", "12345678", "테스트", "010-1234-1234");

            given(buyerRepository.existsByEmail("테스트@테스트.com")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> buyerAuthService.signup(request))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.EMAIL_ALREADY_EXISTS.getMessage());

            verifyNoInteractions(cartRepository);
        }
    }

    @Nested
    @DisplayName("로그인")
    class LoginTest {

        @Test
        @DisplayName("로그인 성공 시 refresh:buyer:{token}에 TTL 7일로 저장하고 user_refreshes:buyer:{id}에 add/expire한다")
        void login_success_savesRefreshTokenWithBuyerScopedKeys() {
            // given
            BuyerLoginRequest request = new BuyerLoginRequest("테스트@테스트.com", "12345678");

            Buyer buyer = Buyer.of("테스트@테스트.com", "비밀번호암호화", "테스트", "010-1234-1234");
            ReflectionTestUtils.setField(buyer, "id", 1L);

            given(buyerRepository.findByEmail("테스트@테스트.com")).willReturn(Optional.of(buyer));
            given(passwordEncoder.matches("12345678", "비밀번호암호화")).willReturn(true);
            given(jwtProvider.generateToken(1L, UserRole.BUYER)).willReturn("test-accessToken");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForSet()).willReturn(setOperations);

            // when
            LoginResult result = buyerAuthService.login(request);
            String issuedRefreshToken = result.refreshToken();

            // then
            assertThat(result.response().accessToken()).isEqualTo("test-accessToken");
            assertThat(issuedRefreshToken).isNotNull();

            verify(valueOperations).set(
                    eq("refresh:buyer:" + issuedRefreshToken), eq(1L), eq(7L), eq(TimeUnit.DAYS));
            verify(setOperations).add(eq("user_refreshes:buyer:1"), eq(issuedRefreshToken));
            verify(redisTemplate).expire(eq("user_refreshes:buyer:1"), eq(7L), eq(TimeUnit.DAYS));
        }

        @Test
        @DisplayName("이메일이 존재하지 않으면 LOGIN_FAILED를 던진다")
        void login_fail_whenEmailNotFound() {
            // given
            BuyerLoginRequest request = new BuyerLoginRequest("테스트@테스트.com", "12345678");

            given(buyerRepository.findByEmail("테스트@테스트.com")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> buyerAuthService.login(request))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.LOGIN_FAILED.getMessage());
        }

        @Test
        @DisplayName("탈퇴한 회원이면 LOGIN_FAILED를 던진다")
        void login_fail_whenBuyerAlreadyDeleted() {
            // given
            BuyerLoginRequest request = new BuyerLoginRequest("테스트@테스트.com", "12345678");

            Buyer buyer = Buyer.of("테스트@테스트.com", "비밀번호암호화", "테스트", "010-1234-1234");
            buyer.delete();

            given(buyerRepository.findByEmail("테스트@테스트.com")).willReturn(Optional.of(buyer));

            // when & then
            assertThatThrownBy(() -> buyerAuthService.login(request))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.LOGIN_FAILED.getMessage());
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 LOGIN_FAILED를 던진다")
        void login_fail_whenPasswordMismatch() {
            // given
            BuyerLoginRequest request = new BuyerLoginRequest("테스트@테스트.com", "틀린비밀번호");

            Buyer buyer = Buyer.of("테스트@테스트.com", "비밀번호암호화", "테스트", "010-1234-1234");

            given(buyerRepository.findByEmail("테스트@테스트.com")).willReturn(Optional.of(buyer));
            given(passwordEncoder.matches("틀린비밀번호", "비밀번호암호화")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> buyerAuthService.login(request))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.LOGIN_FAILED.getMessage());
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class RefreshTest {

        @Test
        @DisplayName("refresh:buyer:{token}이 유효하면 새 토큰을 발급하고 구 키(refresh:{token})는 조회하지 않는다")
        void refresh_success_issuesNewTokenWithoutTouchingLegacyKey() {
            // given
            Buyer buyer = Buyer.of("테스트@테스트.com", "암호화", "테스트", "010-1234-1234");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(valueOperations.getAndDelete("refresh:buyer:old-refresh-token")).willReturn(1L);
            given(buyerRepository.findById(1L)).willReturn(Optional.of(buyer));
            given(jwtProvider.generateToken(1L, UserRole.BUYER)).willReturn("new-accessToken");

            // when
            LoginResult result = buyerAuthService.refresh("old-refresh-token");
            String newRefreshToken = result.refreshToken();

            // then
            assertThat(result.response().accessToken()).isEqualTo("new-accessToken");
            assertThat(newRefreshToken).isNotNull().isNotEqualTo("old-refresh-token");

            verify(valueOperations).set(
                    eq("refresh:buyer:" + newRefreshToken), eq(1L), eq(7L), eq(TimeUnit.DAYS));
            verify(setOperations).remove(eq("user_refreshes:buyer:1"), eq("old-refresh-token"));
            verify(setOperations).add(eq("user_refreshes:buyer:1"), eq(newRefreshToken));
            verify(redisTemplate).expire(eq("user_refreshes:buyer:1"), eq(7L), eq(TimeUnit.DAYS));
            verify(valueOperations, never()).getAndDelete("refresh:old-refresh-token");
        }

        @Test
        @DisplayName("신규 키와 구 키 모두 없으면 TOKEN_EXPIRED를 던지고 구 키 정리를 1회 시도한다")
        void refresh_fail_whenBothNewAndLegacyKeysMissing() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.getAndDelete("refresh:buyer:expired-token")).willReturn(null);
            given(valueOperations.getAndDelete("refresh:expired-token")).willReturn(null);

            // when & then
            assertThatThrownBy(() -> buyerAuthService.refresh("expired-token"))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.TOKEN_EXPIRED.getMessage());

            verify(valueOperations).getAndDelete("refresh:buyer:expired-token");
            verify(valueOperations, org.mockito.Mockito.times(1)).getAndDelete("refresh:expired-token");
            verifyNoInteractions(buyerRepository);
        }

        @Test
        @DisplayName("[회귀] 판매자용 세그먼트(refresh:seller:{token})에만 값이 있어도 구매자 refresh는 거절되고 buyerRepository를 조회하지 않는다")
        void refresh_fail_whenTokenBelongsToSellerNamespace_thenRejectedWithoutBuyerLookup() {
            // given
            // 실제 값은 "refresh:seller:{token}"에 있고 "refresh:buyer:{token}"에는 없는 크로스-롤 상황을 재현한다.
            // BuyerAuthService.refresh()는 buyer 세그먼트 키만 조회하므로 seller 키는 절대 호출되지 않는다(lenient로 미사용 스텁 허용).
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            lenient().when(valueOperations.getAndDelete("refresh:seller:cross-role-token")).thenReturn(99L);
            given(valueOperations.getAndDelete("refresh:buyer:cross-role-token")).willReturn(null);
            given(valueOperations.getAndDelete("refresh:cross-role-token")).willReturn(null);

            // when & then
            assertThatThrownBy(() -> buyerAuthService.refresh("cross-role-token"))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.TOKEN_EXPIRED.getMessage());

            verify(valueOperations).getAndDelete("refresh:buyer:cross-role-token");
            verify(valueOperations, never()).getAndDelete("refresh:seller:cross-role-token");
            verifyNoInteractions(buyerRepository);
        }

        @Test
        @DisplayName("구 형식 키(refresh:{token})에만 값이 있어도 신뢰하지 않고 TOKEN_EXPIRED를 던진다")
        void refresh_fail_whenOnlyLegacyKeyHasValue_thenRejectedWithoutDualRead() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.getAndDelete("refresh:buyer:legacy-only-token")).willReturn(null);
            given(valueOperations.getAndDelete("refresh:legacy-only-token")).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> buyerAuthService.refresh("legacy-only-token"))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.TOKEN_EXPIRED.getMessage());

            verify(valueOperations).getAndDelete("refresh:legacy-only-token");
            verifyNoInteractions(buyerRepository);
        }

        @Test
        @DisplayName("토큰은 유효하지만 대상 회원이 없으면 BUYER_NOT_FOUND를 던진다")
        void refresh_fail_whenBuyerNotFound() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.getAndDelete("refresh:buyer:valid-token")).willReturn(1L);
            given(buyerRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> buyerAuthService.refresh("valid-token"))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.BUYER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("대상 회원이 탈퇴 상태면 BUYER_ALREADY_DELETED를 던진다")
        void refresh_fail_whenBuyerAlreadyDeleted() {
            // given
            Buyer buyer = Buyer.of("테스트@테스트.com", "암호화", "테스트", "010-1234-1234");
            buyer.delete();

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.getAndDelete("refresh:buyer:valid-token")).willReturn(1L);
            given(buyerRepository.findById(1L)).willReturn(Optional.of(buyer));

            // when & then
            assertThatThrownBy(() -> buyerAuthService.refresh("valid-token"))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.BUYER_ALREADY_DELETED.getMessage());
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class LogoutTest {

        @Test
        @DisplayName("정상 로그아웃 시 활성 refresh 토큰을 모두 삭제하고 access 토큰을 blacklist:{token}에 등록한다")
        void logout_success_deletesActiveRefreshTokensAndBlacklistsAccessToken() {
            // given
            String accessToken = "buyer-access-token";
            given(jwtProvider.getRole(accessToken)).willReturn(UserRole.BUYER);
            given(jwtProvider.getUserId(accessToken)).willReturn(1L);
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("user_refreshes:buyer:1"))
                    .willReturn(Set.of("token-a", "token-b"));
            given(jwtProvider.getRemainingExpiration(accessToken)).willReturn(60_000L);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            buyerAuthService.logout(accessToken);

            // then
            verify(redisTemplate).delete("refresh:buyer:token-a");
            verify(redisTemplate).delete("refresh:buyer:token-b");
            verify(redisTemplate).delete("user_refreshes:buyer:1");
            verify(valueOperations).set(
                    eq("blacklist:buyer-access-token"), eq("logout"), eq(60_000L), eq(TimeUnit.MILLISECONDS));
        }

        @Test
        @DisplayName("[회귀] access 토큰의 role이 BUYER가 아니면 FORBIDDEN을 던지고 Redis와 전혀 상호작용하지 않는다")
        void logout_fail_whenRoleMismatch_thenThrowsForbiddenWithoutRedisInteraction() {
            // given
            String accessToken = "seller-access-token";
            given(jwtProvider.getRole(accessToken)).willReturn(UserRole.SELLER);

            // when & then
            assertThatThrownBy(() -> buyerAuthService.logout(accessToken))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.FORBIDDEN.getMessage());

            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("활성 refresh 토큰 Set이 비어 있으면 개별 삭제 없이 blacklist 등록만 수행한다")
        void logout_whenActiveTokenSetEmpty_thenOnlyBlacklistsAccessToken() {
            // given
            String accessToken = "buyer-access-token";
            given(jwtProvider.getRole(accessToken)).willReturn(UserRole.BUYER);
            given(jwtProvider.getUserId(accessToken)).willReturn(1L);
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("user_refreshes:buyer:1")).willReturn(Collections.emptySet());
            given(jwtProvider.getRemainingExpiration(accessToken)).willReturn(60_000L);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            buyerAuthService.logout(accessToken);

            // then
            verify(redisTemplate, never()).delete(anyString());
            verify(valueOperations).set(
                    eq("blacklist:buyer-access-token"), eq("logout"), eq(60_000L), eq(TimeUnit.MILLISECONDS));
        }

        @Test
        @DisplayName("활성 refresh 토큰 Set이 null이어도 예외 없이 blacklist 등록만 수행한다")
        void logout_whenActiveTokenSetNull_thenOnlyBlacklistsAccessToken() {
            // given
            String accessToken = "buyer-access-token";
            given(jwtProvider.getRole(accessToken)).willReturn(UserRole.BUYER);
            given(jwtProvider.getUserId(accessToken)).willReturn(1L);
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("user_refreshes:buyer:1")).willReturn(null);
            given(jwtProvider.getRemainingExpiration(accessToken)).willReturn(60_000L);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            buyerAuthService.logout(accessToken);

            // then
            verify(redisTemplate, never()).delete(anyString());
            verify(valueOperations).set(
                    eq("blacklist:buyer-access-token"), eq("logout"), eq(60_000L), eq(TimeUnit.MILLISECONDS));
        }

        @Test
        @DisplayName("access 토큰의 남은 만료시간이 0이면(경계값) blacklist에 등록하지 않는다")
        void logout_whenRemainingExpirationIsZero_thenSkipsBlacklist() {
            // given
            String accessToken = "buyer-access-token";
            given(jwtProvider.getRole(accessToken)).willReturn(UserRole.BUYER);
            given(jwtProvider.getUserId(accessToken)).willReturn(1L);
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("user_refreshes:buyer:1")).willReturn(Collections.emptySet());
            given(jwtProvider.getRemainingExpiration(accessToken)).willReturn(0L);

            // when
            buyerAuthService.logout(accessToken);

            // then
            verifyNoInteractions(valueOperations);
        }

        @Test
        @DisplayName("access 토큰의 남은 만료시간이 1ms여도(경계값) blacklist에 등록한다")
        void logout_whenRemainingExpirationIsOneMillisecond_thenBlacklists() {
            // given
            String accessToken = "buyer-access-token";
            given(jwtProvider.getRole(accessToken)).willReturn(UserRole.BUYER);
            given(jwtProvider.getUserId(accessToken)).willReturn(1L);
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("user_refreshes:buyer:1")).willReturn(Collections.emptySet());
            given(jwtProvider.getRemainingExpiration(accessToken)).willReturn(1L);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            buyerAuthService.logout(accessToken);

            // then
            verify(valueOperations).set(
                    eq("blacklist:buyer-access-token"), eq("logout"), eq(1L), eq(TimeUnit.MILLISECONDS));
        }
    }
}
