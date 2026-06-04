# Portfolio Backend Improvement Plan

## Goal

운영 코드를 바로 크게 바꾸기보다, 포트폴리오 신뢰도를 가장 빠르게 올리는 작은 PR 단위로 개선한다. 우선순위는 데이터 정합성, 보안 노출, 실제 DB 검증, 쿼리 성능 증거, 문서화 순서다.

## Phase 1: P1 correctness and security fixes

### 1. 재고 동시성 제어를 실제로 증명하기

- Related findings: 1, 11
- Scope: `ProductRepository`, `BuyerPaymentOrderFacade`, `BuyerOrderService`, 주문 생성 테스트
- Work:
  - Redis 분산락만으로 충분한지, DB pessimistic lock/atomic update/optimistic lock 중 무엇을 보조 방어선으로 둘지 결정한다.
  - 메서드명과 주석이 실제 구현과 일치하도록 정리한다.
  - PostgreSQL 기반 동시성 테스트를 추가한다.
- Verification:

```bash
./gradlew test --tests '*BuyerOrderServiceTest'
./gradlew test
```

### 2. PostgreSQL Flyway migration 검증 테스트 추가

- Related findings: 2, 11
- Scope: `src/test/resources/application-test.yml`, Testcontainers PostgreSQL 기반 테스트, Flyway migration
- Work:
  - H2 단위/슬라이스 테스트는 유지한다.
  - 별도 integration test profile 또는 테스트 클래스로 PostgreSQLContainer를 띄우고 Flyway를 실제 실행한다.
  - payment partial unique index, default address unique index, outbox polling index, `pg_trgm` extension을 검증한다.
- Verification:

```bash
./gradlew test --tests '*Migration*'
./gradlew test
```

### 3. 상품 목록 N+1 방지와 쿼리 수 검증

- Related findings: 3
- Scope: `ProductRepository`, `BuyerProductService`, `ProductDetailResponse`
- Work:
  - 목록 응답에 필요한 sellerId/categoryId를 projection으로 조회하거나 entity graph/fetch plan을 명시한다.
  - 단건 조회와 목록 조회의 fetch 전략을 분리한다.
  - query count 테스트 또는 Hibernate statistics 기반 테스트를 추가한다.
- Verification:

```bash
./gradlew test --tests '*BuyerProductServiceTest'
./gradlew test
```

### 4. Pageable 최대 size 제한 도입

- Related findings: 5
- Scope: Spring MVC 설정, pageable을 받는 controller 테스트/RestDocs
- Work:
  - 글로벌 max page size를 설정한다.
  - 기본값과 최대값을 API 문서에 반영한다.
  - `size` 초과 요청이 기대한 방식으로 제한되거나 실패하는지 테스트한다.
- Verification:

```bash
./gradlew test --tests '*ControllerTest'
./gradlew asciidoctor
```

### 5. S3 업로드와 DB 저장의 보상 전략 정리

- Related findings: 7
- Scope: `SellerProductService.uploadProductImage`, `S3UploadService`, 상품 이미지 테스트
- Work:
  - DB 저장 실패 시 S3 객체 삭제 보상 또는 outbox 기반 확정 전략 중 하나를 선택한다.
  - 트랜잭션 안에서 긴 외부 I/O를 수행하는 현재 구조의 리스크를 줄인다.
  - 실패 케이스 테스트를 추가한다.
- Verification:

```bash
./gradlew test --tests '*SellerProductServiceTest'
./gradlew test
```

### 6. Actuator 접근 정책과 rate limit IP 정책 정리

- Related findings: 9, 10
- Scope: `SecurityConfig`, `LoginRateLimitFilter`, 보안 테스트, infra/docs
- Work:
  - `/actuator/health`와 `/actuator/prometheus` 접근 정책을 분리한다.
  - Prometheus scrape가 내부망/ALB rule로 제한되는지 코드 또는 문서 근거를 남긴다.
  - `X-Forwarded-For` trusted proxy 정책과 로그인 실패 status 정책을 테스트로 고정한다.
- Verification:

```bash
./gradlew test --tests '*LoginRateLimitFilterTest'
./gradlew test --tests '*AuthControllerTest'
./gradlew test
```

## Phase 2: P2 API and performance polish

### 7. 상품 검색 성능을 한계 또는 개선으로 명확히 만들기

- Related findings: 4
- Scope: `ProductRepository.findByKeyword`, Flyway migration, README performance section
- Work:
  - 지금 바로 구현하지 않는다면 README의 known limitations에 `%keyword%` LIKE 한계를 적는다.
  - 구현한다면 products.name/description에 trigram 또는 full-text index를 추가한다.
  - `EXPLAIN ANALYZE` 전후 결과를 문서화한다.
- Verification:

```bash
./gradlew test
```

Optional manual verification:

```sql
EXPLAIN ANALYZE SELECT * FROM products WHERE name LIKE '%keyword%' OR description LIKE '%keyword%';
```

### 8. 결제 mock flow와 실서비스 confirm/webhook flow 분리 계획 문서화

- Related findings: 6
- Scope: `BuyerPaymentFacade`, `BuyerPaymentController`, README/API docs
- Work:
  - 현재 구현이 mock PG 시나리오임을 문서에 명확히 쓴다.
  - 실서비스 전환 시 결제 생성, 결제 승인 확인, webhook, 멱등성 처리를 어떻게 나눌지 설계 메모를 추가한다.
  - 이후 코드 변경 PR에서는 confirm endpoint 분리를 검토한다.
- Verification:

```bash
./gradlew asciidoctor
./gradlew test --tests '*BuyerPayment*'
```

### 9. ID 입력 검증 일관화

- Related findings: 8
- Scope: restock subscription/notification DTO와 path variable 검증
- Work:
  - ID 필드에 `@Positive`를 일관되게 적용한다.
  - path variable 검증이 필요한 controller에 `@Validated`를 적용한다.
  - 0, 음수, null 케이스를 controller test에 추가한다.
- Verification:

```bash
./gradlew test --tests '*Restock*ControllerTest'
./gradlew test
```

## Phase 3: Portfolio documentation upgrade

### 10. README 상단을 백엔드 포트폴리오용으로 재구성

- Related findings: 12
- Scope: `README.md`, `docs/asciidoc/**`, k6/observability docs
- Work:
  - 첫 화면에 프로젝트 목적, 핵심 기술 스택, 아키텍처 경계, 주요 API, 검증 명령을 추가한다.
  - 핵심 기술 결정 3개를 “문제, 원인, 해결, 결과, 배운 점” 형식으로 정리한다.
  - known limitations를 숨기지 말고 개선 계획과 함께 적는다.
- Verification:

```bash
./gradlew asciidoctor
```

### 11. 성능/관측성 증거를 수치로 남기기

- Related findings: 4, 12
- Scope: `k6/**`, `docker-compose-k6.yml`, `infra-grafana/**`, README
- Work:
  - k6 시나리오별 목적과 목표 p95/p99를 정의한다.
  - 최소 1개 시나리오에 대해 실행 환경, 결과, 병목, 개선 후 결과를 표로 남긴다.
  - Micrometer/Prometheus/Grafana 대시보드가 어떤 운영 질문에 답하는지 적는다.
- Verification:

```bash
docker compose -f docker-compose-k6.yml up --abort-on-container-exit
```

## Suggested PR order

Legend: improvement items 1-6 are P1, items 7-9 are P2, and items 10-11 are P3/documentation evidence work.

1. PR 1: PostgreSQL migration verification test — includes item 2.
2. PR 2: stock concurrency fix + concurrency test — includes item 1.
3. PR 3: product list N+1 fix + query count test — includes item 3.
4. PR 4: security hardening for actuator/rate limit assumptions — includes item 6.
5. PR 5: pageable max size + validation consistency — includes items 4 and 9.
6. PR 6: README portfolio upgrade and performance evidence — includes items 7, 10, and 11; item 7 may remain documented as a known limitation unless implemented immediately.
7. PR 7: S3 upload compensation strategy — includes item 5.
8. PR 8: payment mock/real flow documentation or endpoint split — includes item 8.

## Default verification commands

```bash
./gradlew test
./gradlew asciidoctor
./gradlew test --tests '*BuyerOrderServiceTest'
./gradlew test --tests '*BuyerProductServiceTest'
./gradlew test --tests '*BuyerPaymentServiceTest'
./gradlew test --tests '*LoginRateLimitFilterTest'
docker compose -f docker-compose-k6.yml up --abort-on-container-exit
```

## Baseline verification

- `./gradlew test` passed on 2026-06-03 after the review harness was generated.
- This review did not modify production code.
