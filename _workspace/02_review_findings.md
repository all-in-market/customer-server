# Portfolio Backend Review Findings

## Summary

이 리뷰는 `all-in-market`을 주니어 백엔드 개발자 포트폴리오 관점에서 봤을 때, 코드 자체의 신뢰성과 면접/서류에서 설명 가능한 근거를 함께 평가한 결과다. 현재 프로젝트는 Spring Boot, JPA, Redis/Redisson, JWT, Flyway, RestDocs, k6, Terraform, Micrometer까지 폭이 좋다. 다만 “고트래픽 이커머스 백엔드”라고 주장하기 위해서는 동시성 제어, PostgreSQL 스키마 검증, N+1/쿼리 계획, 보안 노출, 문서 증거를 더 단단히 묶어야 한다.

P0로 볼 즉시 장애급 항목은 정적 리뷰에서 확인되지 않았다. 우선순위는 P1, P2 중심이다.

## Findings

### 1. 상품 재고 차감 경로가 “락 기반 동시성 제어”를 주장하지만 DB 락은 적용되어 있지 않다

- Finding: 주문 생성 시 상품을 잠그는 메서드명이 `findAllByIdInWithSellerWithLock`이고 서비스 주석도 락 조회를 말하지만, 실제 `@Lock(LockModeType.PESSIMISTIC_WRITE)`는 주석 처리되어 있다.
- Evidence with file path: `src/main/java/com/example/allinmarket/domain/product/repository/ProductRepository.java:21`, `src/main/java/com/example/allinmarket/domain/product/repository/ProductRepository.java:30`, `src/main/java/com/example/allinmarket/buyer/order/service/BuyerOrderService.java:64`, `src/main/java/com/example/allinmarket/buyer/order/service/BuyerOrderService.java:101`
- Why it matters: 이커머스 포트폴리오에서 재고 동시성은 핵심 질문이다. 현재 Redisson 락은 `BuyerPaymentOrderFacade`에서 잡지만, DB 행 잠금이 빠져 있어 Redis 장애, 락 범위 오류, 다른 코드 경로의 재고 수정, 다중 서비스 확장 상황을 설명하기 어렵다.
- Recommended fix: Redis 락을 유지하더라도 DB 차원의 방어선을 추가한다. 선택지는 `@Lock(PESSIMISTIC_WRITE)` 복구, 조건부 원자 업데이트 쿼리, 또는 낙관적 락 + 재시도다. 그 뒤 PostgreSQL 기반 동시성 테스트를 추가해 동일 상품 재고 1개에 동시 주문 2건 중 1건만 성공함을 검증한다.
- Priority: P1

### 2. PostgreSQL/Flyway 전용 스키마가 테스트에서 검증되지 않는다

- Finding: 운영은 Flyway와 PostgreSQL을 쓰지만 테스트 프로필은 Flyway를 끄고 H2 `create-drop`으로 스키마를 생성한다.
- Evidence with file path: `src/test/resources/application-test.yml:5`, `src/test/resources/application-test.yml:8`, `src/test/resources/application-test.yml:15`, `src/main/resources/db/migration/V6__add_partial_unique_index_on_payments.sql:1`, `src/main/resources/db/migration/V17__add_trigram_index.sql:1`
- Why it matters: 결제 성공 중복 방지, 부분 유니크 인덱스, `pg_trgm` 확장, PostgreSQL 문법은 포트폴리오의 강한 근거가 될 수 있다. 하지만 테스트가 이를 우회하면 “마이그레이션은 있지만 검증은 안 됨”으로 보인다.
- Recommended fix: Testcontainers PostgreSQL 기반의 얇은 migration/schema 테스트를 추가한다. Flyway migration을 실제로 실행하고 `uq_payments_order_success`, `ux_address_default`, outbox polling index, `pg_trgm` 확장 생성 여부를 확인한다.
- Priority: P1

### 3. Product 목록/상세 DTO 변환에서 lazy 연관 접근으로 N+1 위험이 있다

- Finding: `ProductDetailResponse.from`은 `product.getSeller().getId()`와 `product.getCategory().getId()`를 읽지만, 상품 목록 조회 쿼리는 seller/category fetch join이나 projection 없이 `Product` 페이지를 반환한다.
- Evidence with file path: `src/main/java/com/example/allinmarket/domain/product/dto/ProductDetailResponse.java:18`, `src/main/java/com/example/allinmarket/domain/product/dto/ProductDetailResponse.java:21`, `src/main/java/com/example/allinmarket/domain/product/dto/ProductDetailResponse.java:22`, `src/main/java/com/example/allinmarket/buyer/product/service/BuyerProductService.java:48`, `src/main/java/com/example/allinmarket/domain/product/repository/ProductRepository.java:18`, `src/main/java/com/example/allinmarket/domain/product/entity/Product.java:26`, `src/main/java/com/example/allinmarket/domain/product/entity/Product.java:30`
- Why it matters: 상품 목록은 트래픽이 큰 API다. 페이지 크기 10이면 seller/category lazy 접근으로 추가 쿼리가 발생할 수 있고, 캐시가 miss 되는 상황에서는 p95 지연시간에 직접 영향을 준다. 면접에서도 N+1 탐지와 해결 경험을 묻기 좋다.
- Recommended fix: 목록 응답은 DTO projection 또는 seller/category id를 함께 가져오는 fetch plan을 명확히 둔다. Pageable + fetch join의 count query 문제를 피하려면 projection query나 `@EntityGraph(attributePaths = {"seller", "category"})`를 검토한다. 테스트에는 Hibernate statistics 또는 query counting을 붙여 목록 조회 쿼리 수를 검증한다.
- Priority: P1

### 4. 상품 검색은 `%keyword%` LIKE인데 product 검색용 인덱스/측정 자료가 없다

- Finding: 상품 검색은 name/description에 `%keyword%` LIKE를 사용한다. 주석도 풀스캔 위험을 인식하고 있지만, 현재 trigram migration은 `products`가 아니라 `langchain4j_embedding_store`에만 적용되어 있다.
- Evidence with file path: `src/main/java/com/example/allinmarket/domain/product/repository/ProductRepository.java:38`, `src/main/java/com/example/allinmarket/domain/product/repository/ProductRepository.java:41`, `src/main/resources/db/migration/V17__add_trigram_index.sql:3`
- Why it matters: “고트래픽 이커머스”에서 상품 검색은 대표적인 성능 질문이다. 검색 성능을 미해결 상태로 둘 수는 있지만, 그러면 README의 한계와 개선 계획으로 명확히 드러내야 신뢰도가 올라간다.
- Recommended fix: 단기적으로는 README/기술문서에 known limitation으로 기록한다. 구현한다면 PostgreSQL trigram index 또는 full-text search를 products.name/description에 적용하고 `EXPLAIN ANALYZE` 전후 결과를 남긴다.
- Priority: P2

### 5. Pageable 파라미터의 최대 size 제한이 없어 대량 조회 요청에 취약하다

- Finding: 여러 컨트롤러가 `@PageableDefault(size = 10)` 또는 raw `Pageable`을 받지만, 최대 page size 제한이 보이지 않는다.
- Evidence with file path: `src/main/java/com/example/allinmarket/buyer/product/controller/BuyerProductController.java:24`, `src/main/java/com/example/allinmarket/buyer/order/controller/BuyerOrderController.java:50`, `src/main/java/com/example/allinmarket/buyer/restocknotification/controller/RestockNotificationController.java:23`, `src/main/java/com/example/allinmarket/buyer/restocksubscription/controller/BuyerRestockSubscriptionController.java:38`
- Why it matters: 기본값은 제한이 아니다. 클라이언트가 `size=100000` 같은 값을 보내면 DB, Redis cache serialization, 응답 메모리에 부담을 줄 수 있다.
- Recommended fix: `PageableHandlerMethodArgumentResolverCustomizer`로 글로벌 max page size를 설정하거나 컨트롤러별 size 검증을 둔다. RestDocs에도 최대 size 정책을 명시한다.
- Priority: P1

### 6. 결제 확인 파사드가 결제 생성과 외부 결제 조회를 한 요청 안에서 즉시 이어 붙인다

- Finding: `BuyerPaymentFacade.processPayment`는 DB에 PENDING 결제를 생성한 뒤 같은 서버 요청에서 mock/PG 조회와 결제 확정까지 수행한다.
- Evidence with file path: `src/main/java/com/example/allinmarket/buyer/payment/facade/BuyerPaymentFacade.java:28`, `src/main/java/com/example/allinmarket/buyer/payment/facade/BuyerPaymentFacade.java:29`, `src/main/java/com/example/allinmarket/buyer/payment/facade/BuyerPaymentFacade.java:35`, `src/main/java/com/example/allinmarket/buyer/payment/facade/BuyerPaymentFacade.java:40`
- Why it matters: 코드 주석은 실제 결제가 프론트에서 일어난다고 말하지만, API 모델은 생성/확인을 하나의 동기 플로우처럼 보이게 한다. 포트폴리오 관점에서는 결제 생성, PG callback/webhook/confirm, 멱등성 키, 실패 보상 트랜잭션 경계를 분리해서 설명하는 편이 훨씬 설득력 있다.
- Recommended fix: 당장 코드 변경 전에는 README와 API 문서에 “mock 결제 플로우”와 “실서비스라면 분리할 confirm/webhook 플로우”를 명확히 적는다. 이후 구현 시 `POST /payments`와 `POST /payments/{merchantUid}/confirm`을 분리하고 멱등성 테스트를 추가한다.
- Priority: P2

### 7. S3 업로드가 DB 트랜잭션 안에서 실행되어 외부 부작용과 DB 상태가 어긋날 수 있다

- Finding: 상품 이미지 업로드 서비스는 `@Transactional` 메서드 안에서 S3 업로드를 먼저 수행하고, 이후 `ProductImage`를 저장한다.
- Evidence with file path: `src/main/java/com/example/allinmarket/seller/product/service/SellerProductService.java:225`, `src/main/java/com/example/allinmarket/seller/product/service/SellerProductService.java:239`, `src/main/java/com/example/allinmarket/seller/product/service/SellerProductService.java:248`
- Why it matters: S3 업로드 성공 후 DB 저장이 실패하면 고아 객체가 생길 수 있다. 반대로 긴 외부 I/O가 트랜잭션 시간을 늘려 커넥션 점유와 장애 전파를 키울 수 있다.
- Recommended fix: 업로드와 DB 저장의 보상 전략을 문서화하고 구현한다. 예: DB 저장 실패 시 S3 delete 보상, 사전 업로드 후 outbox/비동기 확정, 또는 짧은 트랜잭션 후 외부 작업 분리. 실패 케이스 테스트도 추가한다.
- Priority: P1

### 8. Restock 상품 ID 요청은 양수 검증이 빠져 있다

- Finding: 재입고 구독 요청 DTO는 `@NotNull`만 있고 `@Positive`가 없다. 일부 path variable도 별도 양수 검증 없이 서비스까지 전달된다.
- Evidence with file path: `src/main/java/com/example/allinmarket/domain/restocksubscription/dto/RestockSubscriptionRequest.java:5`, `src/main/java/com/example/allinmarket/domain/restocksubscription/dto/RestockSubscriptionRequest.java:6`, `src/main/java/com/example/allinmarket/buyer/restocknotification/controller/RestockNotificationController.java:45`
- Why it matters: 대부분 DTO에는 `@Positive`가 적용되어 있어 일관성이 깨진다. 작은 입력 검증 구멍이지만 포트폴리오에서는 “검증 정책이 체계적인가”를 보여주는 포인트다.
- Recommended fix: ID 계열 request field와 path variable에 `@Positive`를 일관되게 적용한다. 컨트롤러에 `@Validated`가 필요한 경우 함께 추가하고, 400 응답 테스트를 RestDocs에 남긴다.
- Priority: P2

### 9. 인증/인가 구성에서 actuator metrics/prometheus가 public으로 열려 있다

- Finding: `/actuator/**`가 permitAll이고, 설정에서 `health,info,metrics,prometheus`가 노출되어 있다.
- Evidence with file path: `src/main/java/com/example/allinmarket/common/config/SecurityConfig.java:30`, `src/main/resources/application.yaml:59`, `src/main/resources/application.yaml:63`
- Why it matters: `health`는 public일 수 있지만 metrics/prometheus는 운영 지표와 내부 구조를 노출할 수 있다. 포트폴리오에서는 “모니터링을 붙였다”만큼 “어떻게 보호했다”도 중요하다.
- Recommended fix: Spring Security에서 actuator endpoint별 접근 정책을 나누거나, ALB/VPC/Prometheus scrape 경로에서만 접근 가능하다는 인프라 근거를 문서화한다. 보안 테스트로 public/private endpoint를 검증한다.
- Priority: P1

### 10. 로그인 rate limit의 클라이언트 IP 추출과 실패 판정 정책이 운영 가정에 의존한다

- Finding: `X-Forwarded-For`의 마지막 IP를 사용하고, 로그인 실패 카운트는 응답 status가 400일 때만 증가한다.
- Evidence with file path: `src/main/java/com/example/allinmarket/common/security/LoginRateLimitFilter.java:106`, `src/main/java/com/example/allinmarket/common/security/LoginRateLimitFilter.java:107`, `src/main/java/com/example/allinmarket/common/security/LoginRateLimitFilter.java:165`, `src/main/java/com/example/allinmarket/common/security/LoginRateLimitFilter.java:169`
- Why it matters: 프록시 헤더 신뢰 정책이 명확하지 않으면 rate limit 우회 가능성이 생긴다. 또한 로그인 실패가 401로 바뀌거나 인증 서비스 응답 정책이 달라지면 카운터가 증가하지 않을 수 있다.
- Recommended fix: trusted proxy 설정을 명시하고, 실패 판정은 특정 endpoint의 비성공 인증 응답 정책과 맞춘다. 400/401/Redis 장애/fail-open 정책을 테스트로 고정한다.
- Priority: P1

### 11. 테스트 수는 많지만 핵심 리스크가 mock 중심으로 검증된다

- Finding: 서비스 테스트 다수가 Mockito 기반이며, 실제 DB/락/트랜잭션/마이그레이션을 검증하는 테스트는 제한적이다. Testcontainers 의존성은 있으나 PostgreSQLContainer 사용은 확인되지 않는다.
- Evidence with file path: `src/test/java/com/example/allinmarket/buyer/order/service/BuyerOrderServiceTest.java:42`, `src/test/java/com/example/allinmarket/buyer/payment/service/BuyerPaymentServiceTest.java:45`, `src/test/resources/application-test.yml:5`, `build.gradle:97`, `build.gradle:103`
- Why it matters: 포트폴리오에서는 “테스트가 많다”보다 “위험한 부분을 실제 조건에 가깝게 증명한다”가 더 중요하다. 현재 구조는 비즈니스 분기 검증에는 좋지만, 동시성/DB 제약/성능 쿼리 신뢰도는 약하다.
- Recommended fix: 기존 단위 테스트는 유지하고, 작은 수의 고가치 통합 테스트를 추가한다. 우선순위는 stock race, payment unique success, Flyway migration, product list query count, actuator authorization이다.
- Priority: P1

### 12. README가 기능 흐름 중심이라 백엔드 역량 증거가 첫 화면에 잘 드러나지 않는다

- Finding: README 초반은 서버 소개와 API 목록, 시퀀스 다이어그램 중심이며 실행 방법, 검증 명령, 아키텍처 경계, 성능 결과, 보안/운영 판단, known limitations가 앞부분에 없다.
- Evidence with file path: `README.md:7`, `README.md:17`, `README.md:45`
- Why it matters: 채용 리뷰어는 몇 분 안에 프로젝트의 난이도와 신뢰도를 판단한다. 현재는 좋은 코드 자산이 많아도 README 첫인상에서 “무엇을 왜 잘했는지”가 덜 보인다.
- Recommended fix: README 상단에 포트폴리오 요약 섹션을 추가한다. 포함 항목은 핵심 문제, 아키텍처, 주요 기술 결정, 검증 명령, 성능/관측성 결과, 보안 정책, 한계와 다음 개선이다.
- Priority: P2

## Positive Signals To Preserve

- Controller/Service/Repository 기본 분리는 전반적으로 명확하다.
- 주문 생성은 `BuyerPaymentOrderFacade`가 장바구니 조회와 Redisson 상품 락 획득을 조율하고, `BuyerOrderService`가 트랜잭션 안에서 주문/주문상품/재고/장바구니 삭제를 처리한다.
- 결제 성공 시 outbox 저장과 상태 변경을 같은 트랜잭션에 묶으려는 방향이 좋다.
- Flyway, RestDocs, k6, Terraform, Micrometer/Grafana 자산은 포트폴리오 차별화 소재다.
- `GlobalExceptionHandler`로 공통 오류 응답을 유지하고, DTO validation도 다수 적용되어 있다.
