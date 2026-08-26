# 영속성 · 동시성 · 이벤트 규칙

> **이 문서의 역할**: DB 스키마를 바꾸거나 트랜잭션·락·비동기 코드를 건드릴 때 지켜야 할 규칙과 기존 구현 위치.
> 설계 근거는 [TRD.md](./TRD.md) §5·§7·§8, 의사결정 로그는 [technical-decisions.md](./technical-decisions.md)를 본다.
>
> 관련: [ARCHITECTURE.md](./ARCHITECTURE.md) · [AUTH.md](./AUTH.md)

---

## 1. 스키마 변경 절차

운영 프로파일은 `ddl-auto: validate`다. **엔티티만 바꾸면 애플리케이션이 기동되지 않는다.**

1. `src/main/resources/db/migration/V{n}__{설명}.sql` 추가 (현재 최신 = `V18__add_product_image.sql`)
2. 엔티티 수정 — `@Column(nullable, length, precision, scale)`이 SQL과 일치해야 한다
3. `./gradlew test`로 Flyway 마이그레이션 테스트(`FlywayMigrationTest`) 통과 확인
4. 채팅 테이블(`realtime_chat_*`)이나 `langchain4j_embedding_store`를 건드리면 **형제 서버 영향도부터 확인**한다(코드는 이 리포지토리에 없다)

### 주의

- Testcontainers 이미지는 `pgvector/pgvector:pg16`이어야 한다. 일반 `postgres:16-alpine`은 `V9`의 `CREATE EXTENSION vector`에서 실패한다([TD-001](./technical-decisions.md), [TR-001](./troubleshooting.md)).
- 마이그레이션은 되돌릴 수 없다. 이미 적용된 파일을 수정하지 말고 새 버전을 추가한다.
- 새 엔티티는 `common/entity`의 `CreatableEntity` / `ModifiableEntity` / `DeletableEntity` 중 하나를 상속해 감사 필드를 얻는다.

---

## 2. 정합성은 DB 제약으로 마무리한다

애플리케이션 검증만으로는 동시 요청에서 규칙이 깨진다. 아래 규칙들은 **DB 제약이 최종 방어선**이다.

| 규칙 | 제약 | 마이그레이션 |
|---|---|---|
| 주문당 성공 결제 1건 | `uq_payments_order_success` — `order_id` 부분 유니크 `WHERE status = 'SUCCESS'` | V6 |
| 구매자당 기본 배송지 1건 | `ux_address_default` — `buyer_id` 부분 유니크 `WHERE is_default = true` | V5 |
| 결제당 환불 1건 | `refunds_payment_id_key` | V1 |
| 판매자·기간별 정산 1건 | `uk_settlement_period` | V10 |
| 정산당 지급 1건 / 지급 키 유일 | `uk_payout_settlement_id`, `uk_payout_payout_key` | V13 |
| 판매자·날짜별 통계/대시보드 1건 | `uk_seller_stat_date`, `uk_seller_dashboard_date` | V1, V2 |
| 사용자·상품별 재입고 구독 1건 | `uk_restock_subscriptions_buyer` | V12 |
| 구매자당 장바구니 1개 | `carts_buyer_id_key` | V1 |

**패턴**: 유니크 제약 위반은 예외가 아니라 **정상적인 경합 결과**로 처리한다. 예를 들어 `SellerSettlementService`는 `ConstraintViolationException`을 잡아 "이미 생성된 정산"으로 간주하고 스킵한다.

새 "~는 하나만" 규칙을 만들 때는 서비스 코드의 `existsBy...` 검사에 더해 유니크(또는 부분 유니크) 인덱스를 반드시 추가한다.

---

## 3. 동시성 전략

지점마다 충돌 빈도와 재시도 비용이 다르므로 전략을 달리 적용한다.

| 지점 | 전략 | 코드 |
|---|---|---|
| 주문 생성 · 재고 차감 | Redisson 분산 락 + **DB 조건부 원자 UPDATE** | `BuyerPaymentOrderFacade`, `ProductRepository.decreaseStockIfEnough` |
| 결제 생성 | 주문 행 비관적 락 + 성공 결제 부분 유니크 | `OrderRepository.findByIdAndBuyerIdWithLock` |
| 결제/환불 상태 변경 | 낙관적 락 `@Version` | `payments.version`, `refunds.version` |
| 정산 생성 | 유니크 제약 + 위반 시 스킵 | `SellerSettlementService` |
| 지급 처리 | 비관적 락 + `payout_key` 멱등 | `PayoutRepository` `@Lock(PESSIMISTIC_WRITE)` |
| 아웃박스 폴링 | 비관적 락 (멀티 인스턴스 중복 방지) | `DashboardOutboxRepository`, `HistoryOutboxRepository` |

### 3.1 재고 차감 경로 (가장 조심할 곳)

```java
// BuyerPaymentOrderFacade
productIds = cartItems.stream().map(...).distinct().sorted().toList();  // ← 정렬: 데드락 방지
for (Long productId : productIds) {
    lock.tryLock(3, TimeUnit.SECONDS);   // "lock:product:{id}", 실패 시 REDIS_LOCK_CONFLICT
}
// BuyerOrderService → ProductRepository
UPDATE Product p SET p.stock = p.stock - :quantity
 WHERE p.id = :productId AND p.stock >= :quantity AND p.status = 'ON_SALE'
// updatedRows == 0 이면 재고 부족 → 예외 → 트랜잭션 롤백
```

**지켜야 할 것**
- 락 획득은 **상품 ID 오름차순**으로. 순서를 바꾸면 주문 간 데드락이 생긴다.
- 해제는 `finally`에서 **역순**으로, `isHeldByCurrentThread()` 확인 후.
- 재고 차감은 **엔티티를 읽고 setter로 빼는 방식으로 바꾸지 않는다.** 조건부 UPDATE의 row count가 Redis 락을 우회한 경로까지 막아 주는 최종 방어선이다([TD-002](./technical-decisions.md)).
- 락은 트랜잭션 **밖**에서 잡고 푼다. 그래서 이 로직이 서비스가 아니라 파사드에 있다.

### 3.2 재고 복구

주문 실패·만료 시 재고 복구는 `buyer/order/service/StockReleaseService`가 담당한다. 만료 배치는 `OrderExpiryScheduler`(60초 주기, 30분 경과 `CREATED` 주문, 기본 100건씩 페이지 처리, `app.scheduling.order-expiry.{chunk-size,max-loops}`로 조정 가능)다. 조회는 `OrderRepository.findByStatusAndCreatedAtBefore`에 `ORDER BY o.id`가 걸려 결정적으로 페이징되며, 한 청크에서 한 건도 성공하지 못하면 루프를 즉시 중단해 실패 건이 스케줄러 스레드를 영구 점유하지 않게 한다.

**복구도 차감과 같은 조건부 원자 UPDATE를 쓴다**([TD-007](./technical-decisions.md)).

```java
// StockReleaseService
Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(...);  // 주문 행 잠금
if (order.getStatus() != OrderStatus.CREATED) return;                       // 멱등 가드
List<OrderItem> items = orderItemRepository.findAllByOrderIdWithProduct(order.getId());
order.fail();                       // clearAutomatically 로 준영속화되기 전에 먼저 반영
// ProductRepository
UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :productId
```

**지켜야 할 것**
- 복구를 **엔티티 setter(`Product.releaseStock()`)로 되돌리지 않는다.** 그 메서드는 제거했다. read-modify-write는 Product 행을 잠그지 않아 동시 차감분을 덮어쓴다(lost update).
- 복구 UPDATE에는 `status`·`deletedAt` 조건을 걸지 않는다. 판매 중지·소프트 삭제된 상품의 주문도 재고를 되돌려야 한다.
- `findByIdForUpdate` → 상태 체크 → `order.fail()`의 **순서와 가드**가 중복 복구를 막는 장치다. 순서를 바꾸지 않는다.
- `order.fail()`은 재고 복구 루프보다 **앞**에 둔다. `increaseStock`의 `clearAutomatically = true`가 영속성 컨텍스트를 비워 더티체킹이 무효화되기 때문이다.


---

## 4. 트랜잭션 규칙

- 서비스 클래스는 `@Transactional(readOnly = true)`를 기본으로 두고, 쓰기 메서드에만 `@Transactional`을 붙인다(기존 코드 관례).
- **자가 호출(self-invocation) 금지**: 같은 클래스 안에서 `this.otherMethod()`로 호출하면 프록시를 거치지 않아 `REQUIRES_NEW`, `@Async`, `@Retryable`이 **조용히 무시된다.** 전파 속성이 다른 로직은 별도 빈으로 분리한다. 대시보드 서비스가 `SellerDashboardService` / `DashboardService` / `SellerDashboardProcessor` / `DashboardRowCreatorService`로 나뉜 이유다(README 트러블슈팅).
- **외부 호출·캐시 갱신을 트랜잭션 안에서 하지 않는다.** 커밋 전에 실행되면 롤백된 변경이 외부로 새어 나간다. 두 가지 방법 중 하나를 쓴다.
  1. `@TransactionalEventListener(phase = AFTER_COMMIT)` — 예: `RestockEventListener`
  2. `TransactionSynchronizationManager.registerSynchronization(...)`의 `afterCommit()` — 예: `SellerSettlementService`의 캐시 버전 증가

---

## 5. 아웃박스 (이벤트 유실 방지)

결제/환불 트랜잭션 안에서 이벤트 행을 **같은 트랜잭션으로** 저장하고, 스케줄러가 폴링해 후처리한다.

| 아웃박스 | 테이블 | 스케줄러 | 재시도 |
|---|---|---|---|
| 대시보드 갱신 | `dashboard_outbox` (`event_type = DASHBOARD_UPDATE`) | `DashboardOutboxScheduler` (60초) | `retry_count`, 기본 상한 `DashboardOutBoxConsts.MAX_RETRY_COUNT = 5`(프로퍼티 `app.scheduling.outbox.max-retry`로 재정의) |
| 거래 이력 | `history_outboxes` (`type = PAYMENT` / `REFUND`) | `HistoryOutboxScheduler` (300초) | `HistoryOutBoxConsts.MAX_RETRY_COUNT = 3` |

- 미처리 행 조회는 **ID만** 조회(`findUnprocessedIds`)하고, 건별 처리(`processSingleEvent(Long)` / `HistoryOutboxService.process(Long)`)에서 `@Lock(PESSIMISTIC_WRITE)`로 그 행만 잠근다. **멀티 인스턴스에서 같은 이벤트를 두 번 처리하지 않기 위한 것**이므로 제거하지 않는다.
  - **주의**: 스케줄러 트랜잭션 안에서 여러 행을 한꺼번에 락을 잡고, 그 안에서 `REQUIRES_NEW`로 같은 행을 다시 UPDATE하는 방식(과거 `DashboardOutboxScheduler`의 버그)은 서스펜드된 외부 트랜잭션의 락을 내부 트랜잭션이 영구히 기다리게 만든다. 스케줄러는 트랜잭션을 걸지 않고 ID만 조회하며, 실제 처리는 건별로 별도 빈에서 `REQUIRES_NEW` 트랜잭션 하나로 완결한다.
  - 폴링 쿼리는 `retry_count < :maxRetryCount` 조건으로 재시도 소진 이벤트를 걸러낸다(헤드 블로킹 방지). "재시도 소진 시 강제로 processed=true 처리"하는 로직은 실패 이벤트를 성공으로 위장시켜 두지 않는다.
- 폴링 쿼리는 `(processed, retry_count, id)` 인덱스(V7)를 탄다. 조회 조건을 바꾸면 인덱스도 함께 본다.
- 전달 보장은 **at-least-once**다. 소비 측 로직은 멱등해야 한다.

**새 부수효과를 추가할 때**: 결제/주문 트랜잭션에 무거운 로직을 직접 붙이지 말고 아웃박스에 적재한다. 그것이 결제 응답 시간과 장애 격리를 지키는 방식이다.

---

## 6. 캐시

| 대상 | 키 | TTL | 코드 |
|---|---|---|---|
| 상품 목록 | `products:search:{page}:{size}:{sort}` | 10분 | `BuyerProductService` — 키워드 검색·10페이지 이상 제외 |
| 정산 목록 | `settlement:{sellerId}:v{version}:{page}:{size}:{sort}` | 10분 | `SellerSettlementService` — 5페이지 이상 제외 |
| 대시보드 | `dashboard:{sellerId}:{today}` | 5분 | `SellerDashboardService` |

- 정산 캐시는 키 삭제가 아니라 **버전 키(`settlement:version:{sellerId}`) 증가**로 무효화한다. 증가는 반드시 `afterCommit()`에서 한다(§4).
- 다수 판매자의 버전을 한 번에 올릴 때는 `executePipelined`로 묶는다.
- 캐시 대상 DTO는 직렬화 가능해야 한다(`PageResponse`를 그대로 저장한다).

---

## 7. 쿼리 성능

- 조회 전용 복잡 쿼리는 Querydsl(`domain/*/repository`의 커스텀 구현)을 쓴다. 설정은 `common/config/QuerydslConfig`.
- 연관 엔티티를 순회하는 코드는 **N+1을 먼저 의심**한다. 배치 집계는 `sumNetSalesGroupBySeller`처럼 group by 한 방으로 처리한 선례를 따른다.
- JDBC URL에 `reWriteBatchedInserts=true`가 켜져 있다(배치 INSERT 최적화).
- HikariCP `maximum-pool-size: 20`. 커넥션을 오래 잡는 로직(외부 호출, 긴 락)을 트랜잭션 안에 두지 않는다.
- 상품 검색은 현재 `LIKE '%keyword%'`다. 개선 시 `pg_trgm` 인덱스와 `EXPLAIN ANALYZE` 비교를 남긴다([TRD.md](./TRD.md) §14).
