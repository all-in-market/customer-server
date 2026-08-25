# API 상세 명세

> **이 문서의 역할**: 전체 엔드포인트 목록, 응답/에러 규약, 각 API를 담당하는 컨트롤러와 REST Docs 소스 위치.
> 요청/응답 필드의 정확한 스펙은 **생성된 REST Docs**(`./gradlew asciidoctor` → `build/docs/asciidoc`)가 정본이다.
>
> 관련: [ARCHITECTURE.md](./ARCHITECTURE.md) · [AUTH.md](./AUTH.md) · [TRD.md](./TRD.md) · [rules/CODE_CONVENTION.md](./rules/CODE_CONVENTION.md)

---

## 1. 공통 응답 규약

### 1.1 응답 봉투 — `common/response/ApiResponse.java`

```json
{
  "success": true,
  "status": 200,
  "message": "데이터 조회에 성공하였습니다.",
  "data": { },
  "timestamp": "2026-08-25T10:00:00"
}
```

모든 컨트롤러는 `ResponseEntity<ApiResponse<T>>`를 반환한다. `data`는 실패 시 `null`이다.

### 1.2 성공 메타데이터 — `common/enums/SuccessEnum.java`

| 상수 | status | message |
|---|---|---|
| `REGISTER_SUCCESS` | 201 | 회원가입에 성공하였습니다. |
| `LOGIN_SUCCESS` | 200 | 로그인에 성공하였습니다. |
| `LOGOUT_SUCCESS` | 200 | 로그아웃에 성공하였습니다. |
| `TOKEN_REFRESHED` | 200 | 액세스 토큰이 재발급되었습니다 |
| `CREATE_SUCCESS` | 201 | 데이터 생성에 성공하였습니다. |
| `READ_SUCCESS` | 200 | 데이터 조회에 성공하였습니다. |
| `UPDATE_SUCCESS` | 200 | 데이터 수정에 성공하였습니다. |
| `DELETE_SUCCESS` | 200 | 데이터 삭제에 성공하였습니다. |

### 1.3 페이지 응답 — `common/response/PageResponse.java`

```json
{
  "content": [],
  "currentPage": 1,
  "totalPages": 10,
  "totalElements": 95,
  "size": 10,
  "isLast": false
}
```

`currentPage`는 **1-base**다(`page.getNumber() + 1`). 요청의 `page` 파라미터는 Spring 기본값인 0-base이므로 혼동하지 않는다.

### 1.4 에러 규약

- 도메인 예외는 `BaseException(ErrorEnum)`을 던지고 `common/config/GlobalExceptionHandler`가 `ApiResponse.fail(...)`로 변환한다.
- 새 에러 코드는 `common/enums/ErrorEnum.java`에 추가한다. 임의의 문자열 메시지를 컨트롤러에서 직접 만들지 않는다.

| 분류 | 대표 코드 |
|---|---|
| 공통 | `INVALID_INPUT`(400), `INVALID_ARGUMENT`(400), `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404), `DATA_CONFLICT`(409), `INTERNAL_SERVER_ERROR`(500) |
| 인프라 | `LOCK_ACQUISITION_FAILED`(500), `REDIS_UNAVAILABLE`(503), `REDIS_LOCK_CONFLICT`, `REDIS_LOCK_INTERRUPTED` |
| 인증 | `LOGIN_RATE_LIMITED` |
| 주문 | `ORDER_NOT_FOUND`(404), `ORDER_ALREADY_COMPLETED`(400), `ORDER_NOT_CANCELLABLE`(400), `ORDER_NOT_PAYABLE`(400), `ORDER_NOT_REFUNDABLE`(409) |
| 장바구니 | `CART_NOT_FOUND`(404), `CART_ITEMS_EMPTY`(404), `CART_ITEMS_NOT_FOUND`(404), `INVALID_CART_ITEM_OWNER`(403), `INVALID_ORDER_CART_ITEMS`(404) |
| 상품 | `PRODUCT_NOT_FOUND`(404) |

### 1.5 DTO 규약

- 요청 DTO: `XxxCreateRequest` / `XxxUpdateRequest` — `record`, `@Valid`로 검증
- 응답 DTO: `XxxDetailResponse` — `record` + 정적 팩토리 `from(entity)`. 다건은 별도 리스트 DTO를 만들지 않고 `List<XxxDetailResponse>`로 감싼다
- 검증: `@NotNull` + 범위 애노테이션(`@PositiveOrZero`, `@Digits`), 문자열은 `@NotBlank`. **금액은 0을 허용**하므로 `@Positive`가 아니라 `@PositiveOrZero`를 쓴다
- 엔티티는 같은 제약을 `@Column(nullable = false, precision = ..., scale = ...)`로 미러링한다

---

## 2. 구매자 API

인증 헤더: `Authorization: Bearer {accessToken}` (공개 API 제외). 역할은 [AUTH.md](./AUTH.md) 참고.

### 2.1 인증 — `buyer/auth/controller/BuyerAuthController` · `/auth`

| Method | Path | 설명 | 접근 |
|---|---|---|---|
| POST | `/auth/signup` | 구매자 가입 | 공개 |
| POST | `/auth/login` | 로그인 (rate limit 적용) | 공개 |
| POST | `/auth/refresh` | 토큰 재발급 (RTR) | 공개 (쿠키 필요) |
| POST | `/auth/logout` | 로그아웃 (블랙리스트 등록) | 인증 필요 |

### 2.2 프로필 — `buyer/me/controller/BuyerMeController` · `/buyers/me`

| Method | Path | 설명 |
|---|---|---|
| GET | `/buyers/me` | 내 정보 조회 |
| PUT | `/buyers/me` | 내 정보 수정 |

### 2.3 배송지 — `buyer/address/controller/BuyerAddressController` · `/addresses`

| Method | Path | 설명 |
|---|---|---|
| POST | `/addresses` | 배송지 등록 |
| GET | `/addresses` | 배송지 전체 조회 (`List<AddressDetailResponse>`) |
| PUT | `/addresses/{addressId}` | 배송지 수정 |
| DELETE | `/addresses/{addressId}` | 배송지 삭제 |

> 기본 배송지는 구매자당 1개만 존재할 수 있다(부분 유니크 인덱스 `ux_address_default`).

### 2.4 상품 / 카테고리 — `buyer/product`, `buyer/category`

| Method | Path | 설명 | 접근 |
|---|---|---|---|
| GET | `/products` | 상품 목록 (`Pageable`, `keyword` 선택) | 공개 |
| GET | `/products/{productId}` | 상품 상세 | 공개 |
| GET | `/categories` | 카테고리 전체 조회 | 공개 |

> `keyword`가 있으면 캐시를 타지 않고 `LIKE` 검색을 수행한다. 키워드가 없는 **10페이지 미만** 요청만 Redis 캐시(TTL 10분) 대상이다.

### 2.5 장바구니 — `buyer/cart/controller/BuyerCartController` · `/carts`

| Method | Path | 설명 |
|---|---|---|
| POST | `/carts/items` | 장바구니 상품 추가 |
| GET | `/carts` | 장바구니 조회 |
| PUT | `/carts/items/{productId}` | 수량 변경 |
| DELETE | `/carts/items/{productId}` | 항목 삭제 |

### 2.6 주문 — `buyer/order/controller/BuyerOrderController` · `/orders`

| Method | Path | 설명 |
|---|---|---|
| POST | `/orders` | 주문 생성 (선택한 `cartItemIds` + `addressId`) |
| GET | `/orders` | 주문 목록 (`Pageable`, `status` 필터) |
| GET | `/orders/{orderId}` | 주문 상세 (주문 상품 포함) |

> `POST /orders`는 `BuyerPaymentOrderFacade`를 거친다 — 상품별 Redisson 락 → 재고 조건부 차감 → 주문 저장. 상세는 [PERSISTENCE.md](./PERSISTENCE.md) §동시성.

### 2.7 결제 — `buyer/payment/controller/BuyerPaymentController` · `/payments`

| Method | Path | 설명 |
|---|---|---|
| POST | `/payments` | 결제 생성 + 승인 확인 (**현재 Mock PG**) |
| GET | `/payments` | 결제 목록 (`Pageable`) |
| GET | `/payments/{paymentId}` | 결제 상세 |

> 한 요청에서 생성과 승인을 함께 처리한다. 실 PG 연동 시 confirm/webhook 분리가 필요하다([TRD.md](./TRD.md) §14).

### 2.8 환불 — `buyer/refund/controller/BuyerRefundController`

| Method | Path | 설명 |
|---|---|---|
| POST | `/orders/{orderId}/refunds` | 환불 신청 |
| GET | `/refunds` | 환불 목록 (`Pageable`) |
| GET | `/refunds/{refundId}` | 환불 상세 |

> 주문이 `PAID` 또는 `DELIVERED`일 때만 신청 가능하며, 결제 1건당 환불 1건이다.

### 2.9 재입고 구독 — `buyer/restocksubscription` · `/restock-subscriptions`

| Method | Path | 설명 |
|---|---|---|
| POST | `/restock-subscriptions` | 재입고 알림 신청 |
| GET | `/restock-subscriptions/me` | 내 구독 목록 (`Pageable`) |
| GET | `/restock-subscriptions/me/{productId}` | 특정 상품 구독 조회 |
| DELETE | `/restock-subscriptions/{productId}` | 구독 취소 |

### 2.10 재입고 알림 — `buyer/restocknotification` · `/restock-notifications`

| Method | Path | 설명 |
|---|---|---|
| GET | `/restock-notifications/me` | 내 알림 목록 (`Pageable`) |
| PUT | `/restock-notifications/me` | 전체 읽음 처리 |
| PUT | `/restock-notifications/{productId}` | 특정 상품 알림 읽음 처리 |

---

## 3. 판매자 API

`/seller/**`, `/sellers/**`는 모두 `ROLE_SELLER`가 필요하다.

### 3.1 인증 — `seller/auth/controller/SellerAuthController` · `/seller/auth`

| Method | Path | 설명 | 접근 |
|---|---|---|---|
| POST | `/seller/auth/signup` | 판매자 가입 (`status = PENDING`) | 공개 |
| POST | `/seller/auth/login` | 로그인 — **`APPROVED`가 아니면 거부** | 공개 |
| POST | `/seller/auth/refresh` | 토큰 재발급 | 공개 (쿠키 필요) |
| POST | `/seller/auth/logout` | 로그아웃 | `ROLE_SELLER` |

### 3.2 프로필 — `seller/me/controller/SellerMeController` · `/sellers/me`

| Method | Path | 설명 |
|---|---|---|
| GET | `/sellers/me` | 내 판매자 정보 조회 |
| PUT | `/sellers/me` | 내 판매자 정보 수정 |

### 3.3 상품 — `seller/product/controller/SellerProductController` · `/seller/products`

| Method | Path | 설명 |
|---|---|---|
| POST | `/seller/products` | 상품 등록 |
| GET | `/seller/products` | 내 상품 목록 (`Pageable`) |
| PUT | `/seller/products/{productId}` | 상품 수정 |
| DELETE | `/seller/products/{productId}` | 상품 삭제 (soft delete) |
| PUT | `/seller/products/{productId}/stock` | 재고 수정 — **재입고 이벤트 트리거 지점** |
| POST | `/seller/products/{productId}/images` | 이미지 업로드 (`multipart/form-data`: `image`, `sortOrder`=0, `representative`=false) |
| GET | `/seller/products/{productId}/images` | 이미지 목록 |

> 모든 쓰기 API는 `product.getSeller().getId()`와 현재 판매자 ID를 비교해 다른 판매자의 상품이면 `FORBIDDEN`을 던진다.

### 3.4 주문 상품 — `seller/orderitem/controller/SellerOrderItemController`

| Method | Path | 설명 |
|---|---|---|
| GET | `/seller/orderitems` | 내 상품이 포함된 주문 상품 목록 (`Pageable`) |

> `order_items.seller_id` 기준으로 필터링한다. 이것이 **판매자별 주문 분리**의 구현 지점이다.

### 3.5 대시보드 / 통계

| Method | Path | 컨트롤러 | 설명 |
|---|---|---|---|
| GET | `/seller/dashboard` | `SellerDashBoardController` | 오늘 대시보드 (캐시 `dashboard:{sellerId}:{today}`, TTL 5분) |
| POST | `/seller/dashboard/refresh` | `SellerDashBoardController` | 캐시 삭제 후 재적재 |
| GET | `/seller/statistics/daily/{date}` | `SellerDailyStatisticsController` | 특정 날짜 통계 |
| GET | `/seller/statistics/summary` | `SellerDailyStatisticsController` | 기간 통계 |

### 3.6 정산 — `seller/settlement/controller/SellerSettlementController`

| Method | Path | 설명 |
|---|---|---|
| GET | `/seller/settlements` | 정산 내역 (`Pageable`) — 버전 키 기반 캐시, **5페이지 이상은 DB 직접 조회** |

> 정산 생성과 지급은 API가 아니라 스케줄러가 수행한다([ARCHITECTURE.md](./ARCHITECTURE.md) §4).

---

## 4. 운영 엔드포인트

| Path | 접근 | 비고 |
|---|---|---|
| `/actuator/health`, `/info`, `/metrics`, `/prometheus` | **현재 `permitAll`** | 노출 정책은 개선 대상([TRD.md](./TRD.md) §14) |

---

## 5. REST Docs

컨트롤러 테스트가 스니펫을 만들고, `docs/asciidoc/**`의 adoc이 이를 포함한다.

| 영역 | adoc |
|---|---|
| 인덱스 | `docs/asciidoc/api-docs-customer.adoc` |
| 구매자 | `docs/asciidoc/buyer/{auth,me,address,cart,category,product,order,payment,restock-subscription,restock-notification}.adoc` |
| 판매자 | `docs/asciidoc/seller/{auth,me,product,orderitem,dashboard,statistics,settlement}.adoc` |

```bash
./gradlew asciidoctor   # test 실행 후 build/docs/asciidoc 생성
./gradlew bootJar       # asciidoctor 결과를 static/docs 로 번들
```

**API를 추가/변경하면**: ① 컨트롤러 테스트에 REST Docs 스니펫을 추가하고 ② 해당 adoc에 포함시키고 ③ 이 문서의 표를 갱신한다.
