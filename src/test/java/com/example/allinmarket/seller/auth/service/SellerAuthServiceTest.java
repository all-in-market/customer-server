package com.example.allinmarket.seller.auth.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.seller.auth.dto.request.SellerLoginRequest;
import com.example.allinmarket.seller.auth.dto.response.SellerLoginResponse;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.seller.repository.SellerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SellerAuthServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private SellerAuthService sellerAuthService;

    @Test
    void 로그인_성공_테스트() {
        SellerLoginRequest request = new SellerLoginRequest("seller@test.com", "password123");

        Seller seller = mock(Seller.class);
        given(seller.getStatus()).willReturn(SellerStatus.APPROVED);
        given(seller.getDeletedAt()).willReturn(null);
        given(seller.getPassword()).willReturn("encodedPassword");
        given(seller.getId()).willReturn(1L);
        given(seller.getRole()).willReturn(UserRole.SELLER);

        given(sellerRepository.findByEmailAndDeletedAtIsNull("seller@test.com")).willReturn(Optional.of(seller));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(jwtProvider.generateToken(1L, UserRole.SELLER)).willReturn("jwt.token.here");

        SellerLoginResponse response = sellerAuthService.login(request);

        assertThat(response.accessToken()).isEqualTo("jwt.token.here");
    }

    @Test
    void 로그인_이메일_없는_판매자_예외_테스트() {
        SellerLoginRequest request = new SellerLoginRequest("notfound@test.com", "password123");

        given(sellerRepository.findByEmailAndDeletedAtIsNull("notfound@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> sellerAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessage(ErrorEnum.SELLER_NOT_FOUND.getMessage());
    }

    @Test
    void 로그인_승인_대기_판매자_예외_테스트() {
        SellerLoginRequest request = new SellerLoginRequest("seller@test.com", "password123");

        Seller seller = mock(Seller.class);
        given(seller.getStatus()).willReturn(SellerStatus.PENDING);

        given(sellerRepository.findByEmailAndDeletedAtIsNull("seller@test.com")).willReturn(Optional.of(seller));

        assertThatThrownBy(() -> sellerAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessage(ErrorEnum.FORBIDDEN.getMessage());
    }

    @Test
    void 로그인_탈퇴한_판매자_예외_테스트() {
        SellerLoginRequest request = new SellerLoginRequest("seller@test.com", "password123");

        Seller seller = mock(Seller.class);
        given(seller.getStatus()).willReturn(SellerStatus.APPROVED);
        given(seller.getDeletedAt()).willReturn(LocalDateTime.now());

        given(sellerRepository.findByEmailAndDeletedAtIsNull("seller@test.com")).willReturn(Optional.of(seller));

        assertThatThrownBy(() -> sellerAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessage(ErrorEnum.SELLER_ALREADY_DELETED.getMessage());
    }

    @Test
    void 로그인_비밀번호_불일치_예외_테스트() {
        SellerLoginRequest request = new SellerLoginRequest("seller@test.com", "wrongPassword");

        Seller seller = mock(Seller.class);
        given(seller.getStatus()).willReturn(SellerStatus.APPROVED);
        given(seller.getDeletedAt()).willReturn(null);
        given(seller.getPassword()).willReturn("encodedPassword");

        given(sellerRepository.findByEmailAndDeletedAtIsNull("seller@test.com")).willReturn(Optional.of(seller));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        assertThatThrownBy(() -> sellerAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessage(ErrorEnum.PASSWORD_MISMATCH.getMessage());
    }
}
