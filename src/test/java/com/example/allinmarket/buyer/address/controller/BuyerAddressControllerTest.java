package com.example.allinmarket.buyer.address.controller;

import com.example.allinmarket.buyer.address.dto.request.AddressCreateRequest;
import com.example.allinmarket.buyer.address.dto.request.AddressUpdateRequest;
import com.example.allinmarket.buyer.address.dto.response.AddressDetailResponse;
import com.example.allinmarket.buyer.address.service.BuyerAddressService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.support.RestDocsControllerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BuyerAddressController.class)
public class BuyerAddressControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private BuyerAddressService buyerAddressService;

    // ────────────────────────────────────────────────────
    // SecurityContext 헬퍼
    // ────────────────────────────────────────────────────

    private void setAuthContext(Long userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    // ────────────────────────────────────────────────────
    // 공통 픽스처
    // ────────────────────────────────────────────────────

    private AddressDetailResponse sampleResponse() {
        return new AddressDetailResponse(
                1L,   // addressId
                10L,  // buyerId
                "홍길동",
                "010-1234-5678",
                "서울특별시 강남구 테헤란로 123 456호",
                true  // isDefault
        );
    }

    // ────────────────────────────────────────────────────
    // 배송지 추가
    // ────────────────────────────────────────────────────

    @Test
    void 배송지_추가_성공_테스트() throws Exception {
        setAuthContext(10L);

        AddressCreateRequest request = new AddressCreateRequest(
                "홍길동", "010-1234-5678", "서울특별시 강남구 테헤란로 123 456호"
        );

        given(buyerAddressService.createAddress(anyLong(), any(AddressCreateRequest.class)))
                .willReturn(sampleResponse());

        mockMvc.perform(post("/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value(SuccessEnum.CREATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.addressId").value(1L))
                .andExpect(jsonPath("$.data.buyerId").value(10L))
                .andExpect(jsonPath("$.data.recipient").value("홍길동"))
                .andExpect(jsonPath("$.data.phone").value("010-1234-5678"))
                .andExpect(jsonPath("$.data.detail").value("서울특별시 강남구 테헤란로 123 456호"))
                .andExpect(jsonPath("$.data.isDefault").value(true))
                .andDo(document("buyer/address/create",
                        requestFields(
                                fieldWithPath("recipient").description("수령인 이름"),
                                fieldWithPath("phone").description("수령인 전화번호"),
                                fieldWithPath("detail").description("상세 주소")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.addressId").description("배송지 ID"),
                                fieldWithPath("data.buyerId").description("구매자 ID"),
                                fieldWithPath("data.recipient").description("수령인 이름"),
                                fieldWithPath("data.phone").description("수령인 전화번호"),
                                fieldWithPath("data.detail").description("상세 주소"),
                                fieldWithPath("data.isDefault").description("기본 배송지 여부"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    void 배송지_추가_실패_유효성_오류_테스트() throws Exception {
        setAuthContext(10L);

        AddressCreateRequest request = new AddressCreateRequest("", "", "");

        mockMvc.perform(post("/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    // ────────────────────────────────────────────────────
    // 배송지 목록 조회
    // ────────────────────────────────────────────────────

    @Test
    void 배송지_목록_조회_성공_테스트() throws Exception {
        setAuthContext(10L);

        AddressDetailResponse second = new AddressDetailResponse(
                2L, 10L, "김철수", "010-9876-5432", "부산광역시 해운대구 센텀로 10", false
        );

        given(buyerAddressService.getAllAddresses(anyLong()))
                .willReturn(List.of(sampleResponse(), second));

        mockMvc.perform(get("/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].addressId").value(1L))
                .andExpect(jsonPath("$.data[0].buyerId").value(10L))
                .andExpect(jsonPath("$.data[0].isDefault").value(true))
                .andExpect(jsonPath("$.data[1].addressId").value(2L))
                .andExpect(jsonPath("$.data[1].isDefault").value(false))
                .andDo(document("buyer/address/list",
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data[].addressId").description("배송지 ID"),
                                fieldWithPath("data[].buyerId").description("구매자 ID"),
                                fieldWithPath("data[].recipient").description("수령인 이름"),
                                fieldWithPath("data[].phone").description("수령인 전화번호"),
                                fieldWithPath("data[].detail").description("상세 주소"),
                                fieldWithPath("data[].isDefault").description("기본 배송지 여부"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    void 배송지_목록_조회_빈_목록_테스트() throws Exception {
        setAuthContext(10L);

        given(buyerAddressService.getAllAddresses(anyLong()))
                .willReturn(List.of());

        mockMvc.perform(get("/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ────────────────────────────────────────────────────
    // 배송지 수정
    // ────────────────────────────────────────────────────

    @Test
    void 배송지_수정_성공_테스트() throws Exception {
        setAuthContext(10L);

        AddressUpdateRequest request = new AddressUpdateRequest(
                "수정된수령인", "010-0000-0000", "수정된 상세 주소", false
        );
        AddressDetailResponse updated = new AddressDetailResponse(
                1L, 10L, "수정된수령인", "010-0000-0000", "수정된 상세 주소", false
        );

        given(buyerAddressService.updateAddress(anyLong(), anyLong(), any(AddressUpdateRequest.class)))
                .willReturn(updated);

        mockMvc.perform(put("/addresses/{addressId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.addressId").value(1L))
                .andExpect(jsonPath("$.data.buyerId").value(10L))
                .andExpect(jsonPath("$.data.recipient").value("수정된수령인"))
                .andExpect(jsonPath("$.data.phone").value("010-0000-0000"))
                .andExpect(jsonPath("$.data.detail").value("수정된 상세 주소"))
                .andExpect(jsonPath("$.data.isDefault").value(false))
                .andDo(document("buyer/address/update",
                        pathParameters(
                                parameterWithName("addressId").description("수정할 배송지 ID")
                        ),
                        requestFields(
                                fieldWithPath("recipient").description("수령인 이름 (변경 시에만 입력)").optional(),
                                fieldWithPath("phone").description("수령인 전화번호 (변경 시에만 입력)").optional(),
                                fieldWithPath("detail").description("상세 주소 (변경 시에만 입력)").optional(),
                                fieldWithPath("isDefault").description("기본 배송지 여부 (변경 시에만 입력)").optional()
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.addressId").description("배송지 ID"),
                                fieldWithPath("data.buyerId").description("구매자 ID"),
                                fieldWithPath("data.recipient").description("수령인 이름"),
                                fieldWithPath("data.phone").description("수령인 전화번호"),
                                fieldWithPath("data.detail").description("상세 주소"),
                                fieldWithPath("data.isDefault").description("기본 배송지 여부"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    void 배송지_수정_실패_존재하지_않는_배송지_테스트() throws Exception {
        setAuthContext(10L);  // ← 인증 세팅 추가: 없으면 SecurityUtils에서 401 발생

        AddressUpdateRequest request = new AddressUpdateRequest(
                "수정된수령인", null, null, null
        );

        given(buyerAddressService.updateAddress(anyLong(), anyLong(), any(AddressUpdateRequest.class)))
                .willThrow(new BaseException(ErrorEnum.ADDRESS_NOT_FOUND));

        mockMvc.perform(put("/addresses/{addressId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.ADDRESS_NOT_FOUND.getStatus()))   // 404
                .andExpect(jsonPath("$.message").value(ErrorEnum.ADDRESS_NOT_FOUND.getMessage())); // "존재하지 않는 주소입니다."
    }

    // ────────────────────────────────────────────────────
    // 배송지 삭제
    // ────────────────────────────────────────────────────

    @Test
    void 배송지_삭제_성공_테스트() throws Exception {
        setAuthContext(10L);

        given(buyerAddressService.removeAddress(anyLong(), anyLong()))
                .willReturn(sampleResponse());

        mockMvc.perform(delete("/addresses/{addressId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.DELETE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.addressId").value(1L))
                .andExpect(jsonPath("$.data.buyerId").value(10L))
                .andDo(document("buyer/address/delete",
                        pathParameters(
                                parameterWithName("addressId").description("삭제할 배송지 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.addressId").description("삭제된 배송지 ID"),
                                fieldWithPath("data.buyerId").description("구매자 ID"),
                                fieldWithPath("data.recipient").description("수령인 이름"),
                                fieldWithPath("data.phone").description("수령인 전화번호"),
                                fieldWithPath("data.detail").description("상세 주소"),
                                fieldWithPath("data.isDefault").description("기본 배송지 여부"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    void 배송지_삭제_실패_존재하지_않는_배송지_테스트() throws Exception {
        setAuthContext(10L);  // ← 인증 세팅 추가: 없으면 SecurityUtils에서 401 발생

        given(buyerAddressService.removeAddress(anyLong(), anyLong()))
                .willThrow(new BaseException(ErrorEnum.ADDRESS_NOT_FOUND));

        mockMvc.perform(delete("/addresses/{addressId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.ADDRESS_NOT_FOUND.getStatus()))   // 404
                .andExpect(jsonPath("$.message").value(ErrorEnum.ADDRESS_NOT_FOUND.getMessage())); // "존재하지 않는 주소입니다."
    }
}