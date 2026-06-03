# Sequential Implementation Roadmap

## 원칙

- 이 로드맵은 구현 순서를 정리한 문서이며, 아직 운영 코드는 변경하지 않는다.
- 각 단계는 작은 커밋 하나로 끝나는 것을 기본값으로 한다.
- 위험한 운영 코드 변경보다 문서, 테스트, 검증 기반을 먼저 만든다.
- 각 단계는 독립적으로 테스트 가능해야 하며, 실패하면 다음 단계로 넘어가지 않는다.

## Commit 1: 리뷰 결과와 개선 범위 문서 고정

- 목적: 앞으로의 구현 기준선을 문서로 명확히 한다.
- 변경 범위:
  - `_workspace/02_review_findings.md`
  - `_workspace/03_improvement_plan.md`
  - `_workspace/04_implementation_roadmap.md`
- 작업:
  - P1/P2 항목을 그대로 유지한다.
  - 구현 전제, 검증 명령, PR 순서를 문서로 합의한다.
- 독립 검증:

```bash
test -f _workspace/02_review_findings.md
test -f _workspace/03_improvement_plan.md
test -f _workspace/04_implementation_roadmap.md
```

- 완료 기준:
  - 운영 코드 변경 없이 리뷰와 로드맵만 존재한다.

## Commit 2: README에 포트폴리오 기준선과 known limitations 추가

- 목적: 위험한 코드 변경 전에 프로젝트의 현재 상태와 개선 방향을 정직하게 드러낸다.
- 변경 범위:
  - `README.md`
  - 필요 시 `docs/asciidoc/**`
- 작업:
  - 프로젝트 목적, 핵심 기술 스택, 아키텍처 경계, 검증 명령을 README 상단에 추가한다.
  - 주요 API와 실행/테스트 방법을 첫 화면 근처에 둔다.
  - 핵심 기술 결정 3개를 “문제, 원인, 해결, 결과, 배운 점” 형식으로 추가한다.
  - known limitations에 재고 동시성, PostgreSQL migration 검증, product 검색 LIKE, actuator 접근 정책을 적는다.
  - 각 limitation에는 대응 PR 또는 개선 계획을 함께 붙인다.
  - 결제 flow가 mock PG 시나리오임을 명시한다.
- 독립 검증:

```bash
./gradlew asciidoctor
```

- 완료 기준:
  - README만 읽어도 현재 구현과 다음 개선이 과장 없이 이해된다.

## Commit 3: PostgreSQL/Flyway migration 검증 테스트 기반 추가

- 목적: 운영 DB 스키마를 실제 PostgreSQL에서 검증하는 테스트 기반을 먼저 만든다.
- 변경 범위:
  - `build.gradle` 필요 시 PostgreSQL Testcontainers 의존성 추가
  - `src/test/java/**/migration/**` 또는 유사한 테스트 패키지
  - 테스트 전용 설정 파일 필요 시 추가
- 작업:
  - PostgreSQLContainer를 사용하는 migration 테스트를 추가한다.
  - Flyway migration이 실제로 성공하는지 확인한다.
  - `uq_payments_order_success`, `ux_address_default`, outbox polling index, `pg_trgm` 확장을 검증한다.
- 독립 검증:

```bash
./gradlew test --tests '*Migration*'
```

- 완료 기준:
  - H2 테스트와 별개로 PostgreSQL migration 검증이 독립 실행된다.

## Commit 4: 재고 동시성 실패 재현 테스트 추가

- 목적: 운영 코드 수정 전에 현재 재고 동시성 리스크를 테스트로 재현한다.
- 변경 범위:
  - 주문 생성 통합 테스트 또는 concurrency 테스트
  - PostgreSQL/Testcontainers 기반 테스트 유틸 필요 시 추가
- 작업:
  - 동일 상품 재고 1개에 대해 동시 주문 2건을 시도하는 테스트를 작성한다.
  - 현재 구현에서 실패하거나 취약점이 드러나는 형태를 먼저 기록한다.
  - Redis 분산락이 테스트에 필요하면 테스트 Redis/Redisson 구성을 명확히 한다.
- 독립 검증:

```bash
./gradlew test --tests '*Order*Concurrency*'
```

- 완료 기준:
  - 고칠 대상이 테스트로 설명된다.

## Commit 5: 재고 동시성 제어 구현

- 목적: 테스트로 확인한 재고 경쟁 조건을 실제로 해결한다.
- 변경 범위:
  - `ProductRepository`
  - `BuyerPaymentOrderFacade`
  - `BuyerOrderService`
  - 관련 테스트
- 작업:
  - DB pessimistic lock, atomic conditional update, optimistic lock 중 하나를 선택해 구현한다.
  - 메서드명과 주석을 실제 구현에 맞춘다.
  - Commit 4의 테스트를 통과시킨다.
- 독립 검증:

```bash
./gradlew test --tests '*Order*Concurrency*'
./gradlew test --tests '*BuyerOrderServiceTest'
```

- 완료 기준:
  - 동시 주문에서 재고가 음수가 되거나 중복 차감되지 않는다.

## Commit 6: 상품 목록 N+1 측정 테스트 추가

- 목적: N+1 개선 전후를 비교할 수 있는 테스트를 먼저 만든다.
- 변경 범위:
  - `BuyerProductService` 또는 repository integration test
  - Hibernate statistics/query count 테스트 설정
- 작업:
  - 상품 목록 조회 시 기대 쿼리 수를 정의한다.
  - seller/category lazy 접근으로 추가 쿼리가 발생하는지 관찰한다.
- 독립 검증:

```bash
./gradlew test --tests '*BuyerProduct*'
```

- 완료 기준:
  - 쿼리 수가 테스트 결과로 드러난다.

## Commit 7: 상품 목록 N+1 개선

- 목적: 트래픽이 큰 상품 목록 API의 fetch plan을 명확히 한다.
- 변경 범위:
  - `ProductRepository`
  - `BuyerProductService`
  - `ProductDetailResponse` 필요 시 projection 도입
- 작업:
  - DTO projection 또는 `@EntityGraph`로 sellerId/categoryId 조회를 안정화한다.
  - Pageable count query 문제가 생기지 않도록 목록/상세 전략을 분리한다.
- 독립 검증:

```bash
./gradlew test --tests '*BuyerProduct*'
./gradlew test
```

- 완료 기준:
  - 상품 목록 조회의 쿼리 수가 의도한 범위 안에 있다.

## Commit 8: Pageable 최대 size 정책 테스트 추가

- 목적: 대량 조회 방어 정책을 코드 변경 전에 테스트로 정의한다.
- 변경 범위:
  - 컨트롤러 테스트
  - RestDocs snippets 필요 시 갱신
- 작업:
  - `size` 초과 요청의 기대 동작을 정한다.
  - 상품, 주문, 알림, 구독 목록 중 대표 컨트롤러 테스트를 추가한다.
- 독립 검증:

```bash
./gradlew test --tests '*ControllerTest'
```

- 완료 기준:
  - 최대 page size 정책이 테스트 요구사항으로 고정된다.

## Commit 9: Pageable 최대 size 정책 구현

- 목적: 목록 API의 과도한 page size 요청을 제한한다.
- 변경 범위:
  - Spring MVC pageable 설정
  - 관련 API 문서
- 작업:
  - 글로벌 `PageableHandlerMethodArgumentResolverCustomizer` 또는 동등한 설정을 추가한다.
  - API 문서에 기본 size와 최대 size를 적는다.
- 독립 검증:

```bash
./gradlew test --tests '*ControllerTest'
./gradlew asciidoctor
```

- 완료 기준:
  - 과도한 page size 요청이 제한되고 문서에 반영된다.

## Commit 10: Actuator와 rate limit 보안 테스트 추가

- 목적: 보안 정책을 운영 코드 변경 전에 테스트로 고정한다.
- 변경 범위:
  - `LoginRateLimitFilterTest`
  - 인증/인가 controller 또는 security 테스트
- 작업:
  - `/actuator/health`와 `/actuator/prometheus` 접근 기대값을 정의한다.
  - `X-Forwarded-For` 처리, 400/401 실패 카운팅, Redis 장애 정책을 테스트한다.
- 독립 검증:

```bash
./gradlew test --tests '*LoginRateLimitFilterTest'
./gradlew test --tests '*AuthControllerTest'
```

- 완료 기준:
  - 보안 정책 변경 전 기대 동작이 테스트로 표현된다.

## Commit 11: Actuator와 rate limit 정책 구현

- 목적: 관측성 endpoint와 로그인 방어 정책의 운영 가정을 명확히 한다.
- 변경 범위:
  - `SecurityConfig`
  - `LoginRateLimitFilter`
  - 필요 시 infra/docs
- 작업:
  - actuator endpoint별 공개 범위를 분리한다.
  - trusted proxy/IP 추출 정책을 정리한다.
  - 로그인 실패 status 정책과 rate limit 카운팅 조건을 일치시킨다.
- 독립 검증:

```bash
./gradlew test --tests '*LoginRateLimitFilterTest'
./gradlew test --tests '*AuthControllerTest'
./gradlew test
```

- 완료 기준:
  - 보안 테스트가 통과하고 README 또는 docs에 운영 가정이 남는다.

## Commit 12: ID 입력 검증 테스트 추가

- 목적: 작은 입력 검증 구멍을 먼저 테스트로 드러낸다.
- 변경 범위:
  - restock subscription/notification controller tests
- 작업:
  - null, 0, 음수 productId 요청 테스트를 추가한다.
  - 기대 응답은 400으로 고정한다.
- 독립 검증:

```bash
./gradlew test --tests '*Restock*ControllerTest'
```

- 완료 기준:
  - ID 검증 정책이 테스트로 표현된다.

## Commit 13: ID 입력 검증 구현

- 목적: DTO와 path variable 검증을 일관화한다.
- 변경 범위:
  - `RestockSubscriptionRequest`
  - restock notification/subscription controllers
  - 필요 시 `@Validated` 적용
- 작업:
  - ID 필드와 path variable에 `@Positive`를 적용한다.
  - RestDocs 요청/응답 문서를 갱신한다.
- 독립 검증:

```bash
./gradlew test --tests '*Restock*ControllerTest'
./gradlew asciidoctor
```

- 완료 기준:
  - 잘못된 ID 요청이 서비스 계층까지 내려가지 않는다.

## Commit 14: S3 업로드 실패 보상 테스트 추가

- 목적: 외부 I/O와 DB 트랜잭션 불일치 리스크를 테스트로 정의한다.
- 변경 범위:
  - `SellerProductServiceTest`
  - 필요 시 `S3UploadService` mock test
- 작업:
  - S3 업로드 성공 후 DB 저장 실패 시 기대 보상 동작을 테스트한다.
  - S3 업로드 실패 시 DB 저장이 일어나지 않음을 테스트한다.
- 독립 검증:

```bash
./gradlew test --tests '*SellerProductServiceTest'
```

- 완료 기준:
  - S3/DB 불일치 상황의 기대 동작이 명확해진다.

## Commit 15: S3 업로드 보상 전략 구현

- 목적: S3 객체와 DB 이미지 레코드의 정합성을 높인다.
- 변경 범위:
  - `SellerProductService`
  - `S3UploadService`
  - 관련 테스트
- 작업:
  - DB 저장 실패 시 S3 delete 보상 또는 외부 작업 분리 전략을 구현한다.
  - 긴 외부 I/O가 트랜잭션을 불필요하게 점유하지 않도록 조정한다.
- 독립 검증:

```bash
./gradlew test --tests '*SellerProductServiceTest'
./gradlew test
```

- 완료 기준:
  - S3와 DB 상태 불일치 리스크가 테스트로 방어된다.

## Commit 16: 상품 검색 성능 한계 문서화

- 목적: 아직 구현하지 않은 검색 성능 이슈를 포트폴리오에서 정직하게 설명한다.
- 변경 범위:
  - `README.md`
  - 필요 시 `docs/**`
- 작업:
  - `%keyword%` LIKE의 한계와 PostgreSQL trigram/full-text/Elasticsearch 대안을 적는다.
  - 추후 성능 측정 계획을 적는다.
- 독립 검증:

```bash
./gradlew asciidoctor
```

- 완료 기준:
  - 검색 성능 이슈가 숨겨진 결함이 아니라 개선 계획으로 보인다.

## Commit 17: 성능/관측성 결과 문서 추가

- 목적: k6, Micrometer, Grafana 자산을 포트폴리오 증거로 연결한다.
- 변경 범위:
  - `README.md`
  - `k6/**` 설명 문서 또는 `docs/**`
- 작업:
  - k6 시나리오 목적, 실행 환경, 목표 p95/p99, 결과 표를 추가한다.
  - Grafana/Micrometer 지표가 어떤 운영 질문에 답하는지 설명한다.
- 독립 검증:

```bash
docker compose -f docker-compose-k6.yml up --abort-on-container-exit
```

- 완료 기준:
  - 성능 개선 주장에 수치와 재현 절차가 붙는다.

## Commit 18: 결제 mock flow와 실서비스 분리 설계 문서화

- 목적: 현재 결제 구현의 mock 성격과 실서비스 전환 설계를 명확히 한다.
- 변경 범위:
  - `README.md`
  - `docs/asciidoc/buyer/payment.adoc`
  - 필요 시 별도 기술 결정 문서
- 작업:
  - 현재 `POST /payments`가 mock PG 확인까지 포함함을 설명한다.
  - 실서비스에서는 create, confirm, webhook, idempotency를 어떻게 분리할지 설계한다.
- 독립 검증:

```bash
./gradlew asciidoctor
./gradlew test --tests '*BuyerPayment*'
```

- 완료 기준:
  - 결제 플로우가 과장 없이 설명되고, 이후 코드 분리 PR의 기준이 생긴다.

## Final full verification

모든 단계가 끝난 뒤 전체 검증을 실행한다.

```bash
./gradlew test
./gradlew asciidoctor
docker compose -f docker-compose-k6.yml up --abort-on-container-exit
```

## Notes

- Commit 3부터는 Testcontainers나 Docker 환경이 필요할 수 있다.
- Docker 기반 검증이 로컬에서 불가능하면 실패 사유와 대체 검증 범위를 README 또는 PR 설명에 남긴다.
- 운영 코드 변경 커밋은 반드시 그보다 앞선 테스트 커밋을 가진다.
