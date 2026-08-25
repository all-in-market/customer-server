package com.example.allinmarket.seller.auth.service;

import com.example.allinmarket.common.auth.dto.LoginResult;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.auth.dto.request.SellerLoginRequest;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.seller.repository.SellerRepository;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SellerAuthServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private SellerDashboardRepository sellerDashboardRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private SellerAuthService sellerAuthService;

    @Nested
    @DisplayName("로그인")
    class LoginTest {

        @Test
        @DisplayName("로그인 성공 시 refresh:seller:{token}에 TTL 7일로 저장하고 user_refreshes:seller:{id}에 add/expire한다")
        void login_success_savesRefreshTokenWithSellerScopedKeys() {
            // given
            SellerLoginRequest request = new SellerLoginRequest("seller@test.com", "password123");

            Seller seller = mock(Seller.class);
            given(seller.getStatus()).willReturn(SellerStatus.APPROVED);
            given(seller.getDeletedAt()).willReturn(null);
            given(seller.getPassword()).willReturn("encodedPassword");
            given(seller.getId()).willReturn(1L);
            given(seller.getRole()).willReturn(UserRole.SELLER);

            given(sellerRepository.findByEmail("seller@test.com")).willReturn(Optional.of(seller));
            given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
            given(jwtProvider.generateToken(1L, UserRole.SELLER)).willReturn("jwt.token.here");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForSet()).willReturn(setOperations);

            // when
            LoginResult result = sellerAuthService.login(request);
            String issuedRefreshToken = result.refreshToken();

            // then
            assertThat(result.response().accessToken()).isEqualTo("jwt.token.here");
            assertThat(issuedRefreshToken).isNotNull();

            verify(valueOperations).set(
                    eq("refresh:seller:" + issuedRefreshToken), eq(1L), eq(7L), eq(TimeUnit.DAYS));
            verify(setOperations).add(eq("user_refreshes:seller:1"), eq(issuedRefreshToken));
            verify(redisTemplate).expire(eq("user_refreshes:seller:1"), eq(7L), eq(TimeUnit.DAYS));
        }

        @Test
        @DisplayName("이메일이 존재하지 않는 판매자면 LOGIN_FAILED를 던진다")
        void login_fail_whenEmailNotFound() {
            // given
            SellerLoginRequest request = new SellerLoginRequest("notfound@test.com", "password123");

            given(sellerRepository.findByEmail("notfound@test.com")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerAuthService.login(request))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.LOGIN_FAILED.getMessage());
        }

        @Test
        @DisplayName("승인 대기 상태의 판매자면 LOGIN_FAILED를 던진다")
        void login_fail_whenSellerStatusPending() {
            // given
            SellerLoginRequest request = new SellerLoginRequest("seller@test.com", "password123");

            Seller seller = mock(Seller.class);
            given(seller.getStatus()).willReturn(SellerStatus.PENDING);

            given(sellerRepository.findByEmail("seller@test.com")).willReturn(Optional.of(seller));

            // when & then
            assertThatThrownBy(() -> sellerAuthService.login(request))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.LOGIN_FAILED.getMessage());
        }

        @Test
        @DisplayName("탈퇴한 판매자면 LOGIN_FAILED를 던진다")
        void login_fail_whenSellerAlreadyDeleted() {
            // given
            SellerLoginRequest request = new SellerLoginRequest("seller@test.com", "password123");

            Seller seller = mock(Seller.class);
            given(seller.getDeletedAt()).willReturn(LocalDateTime.now());

            given(sellerRepository.findByEmail("seller@test.com")).willReturn(Optional.of(seller));

            // when & then
            assertThatThrownBy(() -> sellerAuthService.login(request))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.LOGIN_FAILED.getMessage());
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 LOGIN_FAILED를 던진다")
        void login_fail_whenPasswordMismatch() {
            // given
            SellerLoginRequest request = new SellerLoginRequest("seller@test.com", "wrongPassword");

            Seller seller = mock(Seller.class);
            given(seller.getStatus()).willReturn(SellerStatus.APPROVED);
            given(seller.getDeletedAt()).willReturn(null);
            given(seller.getPassword()).willReturn("encodedPassword");

            given(sellerRepository.findByEmail("seller@test.com")).willReturn(Optional.of(seller));
            given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> sellerAuthService.login(request))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.LOGIN_FAILED.getMessage());
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class RefreshTest {

        @Test
        @DisplayName("refresh:seller:{token}이 유효하면 새 토큰을 발급하고 구 키(refresh:{token})는 조회하지 않는다")
        void refresh_success_issuesNewTokenWithoutTouchingLegacyKey() {
            // given
            Seller seller = mock(Seller.class);
            given(seller.getDeletedAt()).willReturn(null);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(valueOperations.getAndDelete("refresh:seller:old-refresh-token")).willReturn(1L);
            given(sellerRepository.findById(1L)).willReturn(Optional.of(seller));
            given(jwtProvider.generateToken(1L, UserRole.SELLER)).willReturn("new-accessToken");

            // when
            LoginResult result = sellerAuthService.refresh("old-refresh-token");
            String newRefreshToken = result.refreshToken();

            // then
            assertThat(result.response().accessToken()).isEqualTo("new-accessToken");
            assertThat(newRefreshToken).isNotNull().isNotEqualTo("old-refresh-token");

            verify(valueOperations).set(
                    eq("refresh:seller:" + newRefreshToken), eq(1L), eq(7L), eq(TimeUnit.DAYS));
            verify(setOperations).remove(eq("user_refreshes:seller:1"), eq("old-refresh-token"));
            verify(setOperations).add(eq("user_refreshes:seller:1"), eq(newRefreshToken));
            verify(redisTemplate).expire(eq("user_refreshes:seller:1"), eq(7L), eq(TimeUnit.DAYS));
            verify(valueOperations, never()).getAndDelete("refresh:old-refresh-token");
        }

        @Test
        @DisplayName("신규 키와 구 키 모두 없으면 TOKEN_EXPIRED를 던지고 구 키 정리를 1회 시도한다")
        void refresh_fail_whenBothNewAndLegacyKeysMissing() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.getAndDelete("refresh:seller:expired-token")).willReturn(null);
            given(valueOperations.getAndDelete("refresh:expired-token")).willReturn(null);

            // when & then
            assertThatThrownBy(() -> sellerAuthService.refresh("expired-token"))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.TOKEN_EXPIRED.getMessage());

            verify(valueOperations).getAndDelete("refresh:seller:expired-token");
            verify(valueOperations, org.mockito.Mockito.times(1)).getAndDelete("refresh:expired-token");
            verifyNoInteractions(sellerRepository);
        }

        @Test
        @DisplayName("[회귀] 구매자용 세그먼트(refresh:buyer:{token})에만 값이 있어도 판매자 refresh는 거절되고 sellerRepository를 조회하지 않는다")
        void refresh_fail_whenTokenBelongsToBuyerNamespace_thenRejectedWithoutSellerLookup() {
            // given
            // 실제 값은 "refresh:buyer:{token}"에 있고 "refresh:seller:{token}"에는 없는 크로스-롤 상황을 재현한다.
            // SellerAuthService.refresh()는 seller 세그먼트 키만 조회하므로 buyer 키는 절대 호출되지 않는다(lenient로 미사용 스텁 허용).
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            lenient().when(valueOperations.getAndDelete("refresh:buyer:cross-role-token")).thenReturn(99L);
            given(valueOperations.getAndDelete("refresh:seller:cross-role-token")).willReturn(null);
            given(valueOperations.getAndDelete("refresh:cross-role-token")).willReturn(null);

            // when & then
            assertThatThrownBy(() -> sellerAuthService.refresh("cross-role-token"))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.TOKEN_EXPIRED.getMessage());

            verify(valueOperations).getAndDelete("refresh:seller:cross-role-token");
            verify(valueOperations, never()).getAndDelete("refresh:buyer:cross-role-token");
            verifyNoInteractions(sellerRepository);
        }

        @Test
        @DisplayName("구 형식 키(refresh:{token})에만 값이 있어도 신뢰하지 않고 TOKEN_EXPIRED를 던진다")
        void refresh_fail_whenOnlyLegacyKeyHasValue_thenRejectedWithoutDualRead() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.getAndDelete("refresh:seller:legacy-only-token")).willReturn(null);
            given(valueOperations.getAndDelete("refresh:legacy-only-token")).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> sellerAuthService.refresh("legacy-only-token"))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.TOKEN_EXPIRED.getMessage());

            verify(valueOperations).getAndDelete("refresh:legacy-only-token");
            verifyNoInteractions(sellerRepository);
        }

        @Test
        @DisplayName("토큰은 유효하지만 대상 판매자가 없으면 SELLER_NOT_FOUND를 던진다")
        void refresh_fail_whenSellerNotFound() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.getAndDelete("refresh:seller:valid-token")).willReturn(1L);
            given(sellerRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerAuthService.refresh("valid-token"))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.SELLER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("대상 판매자가 탈퇴 상태면 SELLER_ALREADY_DELETED를 던진다")
        void refresh_fail_whenSellerAlreadyDeleted() {
            // given
            Seller seller = mock(Seller.class);
            given(seller.getDeletedAt()).willReturn(LocalDateTime.now());

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.getAndDelete("refresh:seller:valid-token")).willReturn(1L);
            given(sellerRepository.findById(1L)).willReturn(Optional.of(seller));

            // when & then
            assertThatThrownBy(() -> sellerAuthService.refresh("valid-token"))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.SELLER_ALREADY_DELETED.getMessage());
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class LogoutTest {

        @Test
        @DisplayName("정상 로그아웃 시 활성 refresh 토큰을 모두 삭제하고 access 토큰을 blacklist:{token}에 등록한다")
        void logout_success_deletesActiveRefreshTokensAndBlacklistsAccessToken() {
            // given
            String accessToken = "seller-access-token";
            given(jwtProvider.getRole(accessToken)).willReturn(UserRole.SELLER);
            given(jwtProvider.getUserId(accessToken)).willReturn(1L);
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("user_refreshes:seller:1"))
                    .willReturn(Set.of("token-a", "token-b"));
            given(jwtProvider.getRemainingExpiration(accessToken)).willReturn(60_000L);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            sellerAuthService.logout(accessToken);

            // then
            verify(redisTemplate).delete("refresh:seller:token-a");
            verify(redisTemplate).delete("refresh:seller:token-b");
            verify(redisTemplate).delete("user_refreshes:seller:1");
            verify(valueOperations).set(
                    eq("blacklist:seller-access-token"), eq("logout"), eq(60_000L), eq(TimeUnit.MILLISECONDS));
        }

        @Test
        @DisplayName("[회귀] access 토큰의 role이 SELLER가 아니면 FORBIDDEN을 던지고 Redis와 전혀 상호작용하지 않는다")
        void logout_fail_whenRoleMismatch_thenThrowsForbiddenWithoutRedisInteraction() {
            // given
            String accessToken = "buyer-access-token";
            given(jwtProvider.getRole(accessToken)).willReturn(UserRole.BUYER);

            // when & then
            assertThatThrownBy(() -> sellerAuthService.logout(accessToken))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.FORBIDDEN.getMessage());

            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("활성 refresh 토큰 Set이 비어 있으면 개별 삭제 없이 blacklist 등록만 수행한다")
        void logout_whenActiveTokenSetEmpty_thenOnlyBlacklistsAccessToken() {
            // given
            String accessToken = "seller-access-token";
            given(jwtProvider.getRole(accessToken)).willReturn(UserRole.SELLER);
            given(jwtProvider.getUserId(accessToken)).willReturn(1L);
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("user_refreshes:seller:1")).willReturn(Collections.emptySet());
            given(jwtProvider.getRemainingExpiration(accessToken)).willReturn(60_000L);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            sellerAuthService.logout(accessToken);

            // then
            verify(redisTemplate, never()).delete(anyString());
            verify(valueOperations).set(
                    eq("blacklist:seller-access-token"), eq("logout"), eq(60_000L), eq(TimeUnit.MILLISECONDS));
        }

        @Test
        @DisplayName("활성 refresh 토큰 Set이 null이어도 예외 없이 blacklist 등록만 수행한다")
        void logout_whenActiveTokenSetNull_thenOnlyBlacklistsAccessToken() {
            // given
            String accessToken = "seller-access-token";
            given(jwtProvider.getRole(accessToken)).willReturn(UserRole.SELLER);
            given(jwtProvider.getUserId(accessToken)).willReturn(1L);
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("user_refreshes:seller:1")).willReturn(null);
            given(jwtProvider.getRemainingExpiration(accessToken)).willReturn(60_000L);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            sellerAuthService.logout(accessToken);

            // then
            verify(redisTemplate, never()).delete(anyString());
            verify(valueOperations).set(
                    eq("blacklist:seller-access-token"), eq("logout"), eq(60_000L), eq(TimeUnit.MILLISECONDS));
        }

        @Test
        @DisplayName("access 토큰의 남은 만료시간이 0이면(경계값) blacklist에 등록하지 않는다")
        void logout_whenRemainingExpirationIsZero_thenSkipsBlacklist() {
            // given
            String accessToken = "seller-access-token";
            given(jwtProvider.getRole(accessToken)).willReturn(UserRole.SELLER);
            given(jwtProvider.getUserId(accessToken)).willReturn(1L);
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("user_refreshes:seller:1")).willReturn(Collections.emptySet());
            given(jwtProvider.getRemainingExpiration(accessToken)).willReturn(0L);

            // when
            sellerAuthService.logout(accessToken);

            // then
            verifyNoInteractions(valueOperations);
        }

        @Test
        @DisplayName("access 토큰의 남은 만료시간이 1ms여도(경계값) blacklist에 등록한다")
        void logout_whenRemainingExpirationIsOneMillisecond_thenBlacklists() {
            // given
            String accessToken = "seller-access-token";
            given(jwtProvider.getRole(accessToken)).willReturn(UserRole.SELLER);
            given(jwtProvider.getUserId(accessToken)).willReturn(1L);
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("user_refreshes:seller:1")).willReturn(Collections.emptySet());
            given(jwtProvider.getRemainingExpiration(accessToken)).willReturn(1L);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            sellerAuthService.logout(accessToken);

            // then
            verify(valueOperations).set(
                    eq("blacklist:seller-access-token"), eq("logout"), eq(1L), eq(TimeUnit.MILLISECONDS));
        }
    }
}
