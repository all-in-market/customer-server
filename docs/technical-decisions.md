# Technical Decisions

## TD-001 / 2026-06-05: PostgreSQL migration tests use a pgvector-enabled database image

**결정 내용:** 
Flyway migration 검증 테스트의 PostgreSQL Testcontainer 이미지를 일반 `postgres:16-alpine`이 아니라 `pgvector/pgvector:pg16`으로 사용한다.

**이유 / 배경:** 
`V9__add_vector_store.sql`은 `CREATE EXTENSION IF NOT EXISTS vector`와 `vector(1536)` 컬럼을 생성한다. 이 migration은 `langchain4j_embedding_store`와 vector 검색 기능을 위한 실제 운영 스키마이므로 테스트에서도 pgvector 확장이 설치된 PostgreSQL 환경을 사용해야 한다.

**대안으로 고려했던 것 & 그 이유:** 
- `postgres:16-alpine` 유지: 기본 PostgreSQL 환경 검증에는 좋지만 `vector` 확장이 없어 실제 운영 migration을 끝까지 검증할 수 없다.
- `V9__add_vector_store.sql` 제외: 테스트는 통과할 수 있지만 전체 Flyway migration 검증이라는 목적이 깨진다.
- vector store를 별도 Flyway location으로 분리: 선택 기능이라면 가능하지만, 현재는 실제 필요한 기능이므로 기본 migration 검증 대상에 포함하는 편이 맞다.

**영향받는 문서 / 파일:** 
- `src/test/java/com/example/allinmarket/migration/FlywayMigrationTest.java`
- `src/main/resources/db/migration/V9__add_vector_store.sql`
- `docs/troubleshooting.md`

## TD-002 / 2026-06-05: 재고 차감은 Redis lock에 DB atomic conditional update를 보조 방어선으로 둔다

**결정 내용:** 
주문 생성의 재고 차감은 facade의 상품별 Redis 분산 락을 유지하되, DB에서는 `UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?` 형태의 조건부 update를 최종 방어선으로 사용한다. update row count가 `0`이면 재고 부족으로 판단하고 주문 트랜잭션을 rollback한다.

**이유 / 배경:** 
현재 API 경로는 `BuyerPaymentOrderFacade`에서 상품 ID별 Redis lock을 획득한 뒤 `BuyerOrderService.createOrder`를 호출한다. Redis lock은 경합을 앞단에서 줄이는 데 유리하지만, service/DB 경계에는 Redis lock을 우회하거나 Redis 장애/설정 변경이 발생했을 때 재고 정합성을 보장하는 방어선이 없었다. 조건부 update는 긴 DB pessimistic lock을 명시적으로 유지하지 않으면서도 재고가 충분한 경우에만 차감되도록 DB가 원자적으로 보장한다.

**대안으로 고려했던 것 & 그 이유:** 
- Redis lock 단독 유지: 변경 범위와 성능 측면에서는 가장 가볍지만, DB 최종 방어선이 없어 service 직접 호출이나 Redis 장애 정책 변경에 취약하다.
- DB pessimistic lock 사용: 정합성 설명은 단순하지만 고경합 상품에서 lock wait가 커질 수 있고, Redis lock과 함께 사용하면 중복 락으로 인한 성능/운영 복잡도가 증가할 수 있다.
- optimistic lock 사용: 충돌 감지는 가능하지만 주문 생성 실패 재시도 정책과 version 관리가 추가로 필요해 현재 변경 범위보다 크다.

**영향받는 문서 / 파일:** 
- `src/main/java/com/example/allinmarket/domain/product/repository/ProductRepository.java`
- `src/main/java/com/example/allinmarket/buyer/order/service/BuyerOrderService.java`
- `src/test/java/com/example/allinmarket/buyer/order/service/BuyerOrderConcurrencyTest.java`
- `src/test/java/com/example/allinmarket/buyer/order/service/BuyerOrderServiceTest.java`

## TD-003 / 2026-06-10: 상품 목록 N+1 개선은 측정 결과에 따라 skip한다

**결정 내용:** 
Commit 6에서 추가한 Hibernate statistics 기반 query count 테스트 결과, 현재 상품 목록 조회는 `ProductRepository.findAllVisibleProducts`의 content query와 pageable count query 총 2개 쿼리로 실행된다. `ProductDetailResponse`는 seller/category의 id만 읽기 때문에 lazy proxy가 추가 select로 초기화되지 않는다. 따라서 Commit 7의 production fetch-plan 변경은 현 시점에서 skip한다.

**이유 / 배경:** 
로드맵은 N+1 개선 전에 먼저 쿼리 수를 측정하도록 정했다. 측정 결과 현재 DTO 매핑에서는 seller/category lazy relation이 N+1을 만들지 않았다. 이 상태에서 `@EntityGraph`, fetch join, projection을 도입하면 실제 문제를 고치는 것이 아니라 불필요한 구조 변경이 될 수 있고, 특히 pageable 목록 조회에서는 count query나 중복 row 문제를 새로 만들 수 있다.

**대안으로 고려했던 것 & 그 이유:** 
- `@EntityGraph` 또는 fetch join 도입: 현재 non-id relation 필드를 읽지 않아 추가 select가 없으므로 보류한다. Pageable count query와 조합될 때 부작용도 생길 수 있다.
- DTO projection 도입: 목록 DTO가 seller/category 이름 등 non-id 필드를 노출하게 되면 유효한 선택지지만, 현재 응답 필드에서는 필요성이 측정되지 않았다.
- 단건/목록 fetch 전략 분리: 현재 목록 쿼리 수가 의도 범위 안에 있으므로 N+1이 재현되는 변경이 생길 때 다시 판단한다.

**영향받는 문서 / 파일:** 
- `src/test/java/com/example/allinmarket/buyer/product/service/BuyerProductQueryCountTest.java`
- `_workspace/03_improvement_plan.md`
- `_workspace/04_implementation_roadmap.md`
## TD-004 / 2026-08-25: 아웃박스 폴링은 "ID만 조회 → 건별 REQUIRES_NEW" 한 가지 패턴으로 통일한다

**결정 내용:** 
`DashboardOutboxScheduler`를 `HistoryOutboxScheduler`와 같은 패턴으로 맞춘다. 스케줄러에는 트랜잭션을 두지 않고 `findUnprocessedIds(maxRetryCount, Pageable)`로 **ID만** 조회한 뒤, 건별로 `DashboardOutboxService.processSingleEvent(Long)`(`REQUIRES_NEW`)에 위임한다. 행 잠금(`findByIdForUpdate`)과 멱등 가드(`isProcessed()`)는 그 REQUIRES_NEW 트랜잭션 **안에서** 수행한다. 상태 변경 전용이던 `DashboardOutboxStatusService`(별도 REQUIRES_NEW)는 삭제하고 같은 트랜잭션의 더티체킹으로 흡수한다.

**이유 / 배경:** 
기존 `DashboardOutboxScheduler.processOutbox()`는 `@Transactional` 안에서 `findTop100ForUpdate`로 100행에 `PESSIMISTIC_WRITE`를 건 채 유지하고, 그 내부에서 `REQUIRES_NEW` 트랜잭션이 **같은 행을 UPDATE**했다. 내부 트랜잭션이 서스펜드된 외부 트랜잭션의 락을 기다리는데, 이 대기 순환은 애플리케이션 코드를 경유하므로 PostgreSQL 데드락 감지기에 보이지 않는다. `lock_timeout` 설정도 없어 스케줄러 스레드가 영구 블로킹된다. 같은 리포의 `HistoryOutbox` 경로는 처음부터 올바른 패턴이었고, **두 아웃박스가 서로 다른 패턴이었던 것이 문제의 뿌리**였다.

아울러 재시도 소진 시 `markProcessed()`로 실패건을 성공 표시하던 로직을 제거했다. 실패를 유실시키는 동작이었고, `retryCount < :maxRetryCount` 조회 필터가 같은 역할(폴링 대상에서 제외)을 유실 없이 수행한다.

**대안으로 고려했던 것 & 그 이유:** 
- 외부 트랜잭션에 `lock_timeout`만 설정: 영구 블로킹은 막지만 배치가 매 주기 타임아웃으로 실패한다. 원인이 아니라 증상을 다룬다.
- `FOR UPDATE SKIP LOCKED`로 청크 잠금 유지: PostgreSQL에서 가능하지만 자기 차단 구조 자체는 남고, 리포에 이미 검증된 패턴이 있는데 세 번째 패턴을 추가하게 된다.
- 아웃박스를 Kafka 등 외부 브로커로 이전: 의존성이 늘고 이번 결함 수정 범위를 크게 벗어난다(Kafka 의존성은 이미 리포에 있으나 사용처가 0이다).

**영향받는 문서 / 파일:** 
- `src/main/java/com/example/allinmarket/common/scheduler/DashboardOutboxScheduler.java`
- `src/main/java/com/example/allinmarket/common/outbox/service/DashboardOutboxService.java`
- `src/main/java/com/example/allinmarket/common/outbox/repository/DashboardOutboxRepository.java`
- `src/main/java/com/example/allinmarket/common/outbox/consts/DashboardOutBoxConsts.java`
- 삭제: `src/main/java/com/example/allinmarket/common/outbox/service/DashboardOutboxStatusService.java`
- `docs/ARCHITECTURE.md`, `docs/PERSISTENCE.md`, `docs/troubleshooting.md`(TR-003)

## TD-005 / 2026-08-25: 스케줄링은 `SchedulingConfig`로 분리하고 프로퍼티로 켜고 끈다

**결정 내용:** 
`@EnableScheduling`을 `AllInMarketApplication`에서 떼어내 `common/config/SchedulingConfig`로 옮기고 `@ConditionalOnProperty("app.scheduling.enabled", matchIfMissing = true)`를 건다. 기본값은 **켜짐**이고 `application-local.yml`에서만 끈다. 전용 `ThreadPoolTaskScheduler`(poolSize 5)를 함께 등록한다. 백로그 제어용 프로퍼티(`app.scheduling.outbox.chunk-size`·`max-retry`, `app.scheduling.order-expiry.chunk-size`·`max-loops`)를 노출한다.

**이유 / 배경:** 
`19c8fb1 feat: 스케줄링 처리 정지`로 `@EnableScheduling`이 주석 처리된 뒤 3개월 이상 7개 클래스 9개 `@Scheduled` 메서드가 전부 미실행이었다(주문 만료·아웃박스·통계·정산·지급 전체 정지). 코드 주석으로 끄면 프로파일별 제어가 불가능하고 무엇보다 **끈 사실이 문서와 어긋난 채 방치된다**. 프로퍼티로 빼면 k6 부하 테스트나 로컬 실행에서 스케줄러를 배제하려던 원래 니즈를 유지하면서 dev/prod에서는 기본 동작한다.

기본 스케줄러 풀 크기는 1이라 7개 스케줄러가 단일 스레드에 직렬화되고 하나가 막히면 전부 멈춘다. 이번 결함이 정확히 그 형태였으므로 풀 크기를 명시한다.

**대안으로 고려했던 것 & 그 이유:** 
- `matchIfMissing = false`(기본 꺼짐): 안전하지만 "설정을 빠뜨리면 조용히 안 도는" 현재 문제가 형태만 바꿔 남는다.
- 프로파일 애너테이션(`@Profile("!local")`): 프로파일과 스케줄링 여부가 1:1이 아니고(같은 prod에서도 배치 인스턴스만 켜고 싶을 수 있다) 런타임 프로퍼티보다 유연성이 떨어진다.
- 리더 선출(ShedLock 등) 도입: ECS 멀티 인스턴스 중복 실행을 근본 해결하지만 의존성이 늘고, 현재 각 배치는 비관적 락·유니크 제약·상태 전이로 중복을 방어하고 있다. 별도 과제로 남긴다.

**영향받는 문서 / 파일:** 
- `src/main/java/com/example/allinmarket/common/config/SchedulingConfig.java`
- `src/main/java/com/example/allinmarket/AllInMarketApplication.java`
- `src/main/java/com/example/allinmarket/common/scheduler/OrderExpiryScheduler.java`
- `src/main/resources/application.yaml`, `src/main/resources/application-local.yml`
- `docs/ARCHITECTURE.md`

## TD-006 / 2026-08-25: 구매자/판매자 refresh 토큰 키를 분리하고 구 키는 이관하지 않고 거부한다

**결정 내용:** 
Redis 키를 `refresh:{role}:{token}` / `user_refreshes:{role}:{userId}`로 바꾸고, 키 생성은 `common/auth/consts/AuthConsts`의 정적 메서드로만 한다. 구 형식 키(`refresh:{token}`)는 **값을 읽지 않는다** — 신규 키가 miss일 때 구 키를 `getAndDelete`로 지우기만 하고 `TOKEN_EXPIRED`로 거부해 재로그인을 유도한다. 더불어 `SecurityConfig`의 `/auth/logout`을 `authenticated()`에서 `hasRole("BUYER")`로 좁히고, 두 `logout()` 서비스가 access token의 role claim을 재검증한다.

**이유 / 배경:** 
`buyers`와 `sellers`는 각각 독립 IDENTITY 시퀀스(`V1__init.sql`)라 ID가 반드시 겹친다. 그런데 두 `AuthService`가 `refresh:{token}` / `user_refreshes:{userId}`를 문자 그대로 공유하고 저장 값은 userId뿐이었다. `/auth/**`가 `permitAll`이므로 **판매자 refresh token으로 `POST /auth/refresh`를 호출하면 같은 ID의 전혀 다른 구매자 계정 access token이 발급된다.** 역방향도 같고, 로그아웃은 동일 ID 타 페르소나의 세션을 전부 지웠다. 쿠키 `path`가 `/auth` vs `/seller/auth`로 나뉘어 브라우저에서는 자연 발생하지 않지만 **서버가 강제하는 것이 아무것도 없었다.**

**대안으로 고려했던 것 & 그 이유:** 
- **dual-read 이관**(신규 키 miss 시 구 키를 읽어 신규 키로 옮김): 기존 로그인 세션이 유지되는 장점이 있으나, 구 키에는 role 구분자가 없으므로 **읽는 순간 크로스-롤 발급 경로가 TTL 7일 동안 그대로 열려 있다.** 취약점을 닫는 것이 목적인 변경에서 취약점을 유지하는 선택이라 채택하지 않았다.
- 구 키를 아예 무시(코드 없음): 가장 단순하지만 탈취 가능한 키가 7일간 Redis에 방치된다. 지우기만 하는 현재 방식이 코드 한 줄 차이로 그 창을 닫는다.
- 저장 값에 role을 담아 검증(키는 그대로): 키 충돌로 인한 상호 덮어쓰기(`user_refreshes` Set 공유)는 해결되지 않는다.
- 토큰 자체에 페르소나를 인코딩: refresh token이 opaque UUID라는 성질(README §5)을 깨뜨린다.

**남은 확인 사항:** 
같은 Redis를 공유하는 형제 서버(admin/chat 등)가 `refresh:` / `user_refreshes:` 키를 읽는지는 이 리포지토리에서 확인할 수 없다. 읽는다면 해당 리포도 함께 수정해야 한다.

**영향받는 문서 / 파일:** 
- `src/main/java/com/example/allinmarket/common/auth/consts/AuthConsts.java`
- `src/main/java/com/example/allinmarket/buyer/auth/service/BuyerAuthService.java`
- `src/main/java/com/example/allinmarket/seller/auth/service/SellerAuthService.java`
- `src/main/java/com/example/allinmarket/common/config/SecurityConfig.java`
- `docs/AUTH.md` §2·§4.1·§4.2, `docs/TRD.md` §6.3·§14, `README.md` §5

## TD-007 / 2026-08-25: 재고 복구도 조건부 원자 UPDATE로 처리한다 (TD-002의 대칭 완성)

**결정 내용:** 
`ProductRepository.increaseStock(productId, quantity)`(`UPDATE products SET stock = stock + ?`)를 추가하고 `StockReleaseService`가 이를 호출한다. 엔티티의 `Product.releaseStock()`은 **제거**한다. `decreaseStockIfEnough`에는 `flushAutomatically = true`를 추가한다. 별도 `restock_status` 컬럼이나 `max_stock` 상한은 도입하지 않는다.

**이유 / 배경:** 
TD-002는 재고 **차감**에 대해 "조건부 update는 Redis 락을 우회한 경로까지 막아 주는 최종 방어선"이라고 결정했으나, 복구 경로에는 적용되지 않은 반쪽짜리 구조였다. `StockReleaseService`는 `orderRepository.findByIdForUpdate`로 **주문 행만** 잠그고 Product는 잠그지 않은 채 `item.getProduct().releaseStock(...)`로 더티체킹했고, 그 SQL은 절대값 UPDATE다.

```
T1 (주문 생성): UPDATE products SET stock = stock - 3 WHERE id=1 AND stock >= 3   → 100 → 97
T2 (만료 복구): SELECT stock → 100 (T1 이전 스냅샷) → UPDATE products SET stock = 105   ← T1의 차감 3이 소실
```

복구 쿼리에 상태·삭제 조건을 걸지 않은 것은 의도적이다. 판매 중지되거나 소프트 삭제된 상품의 주문도 만료 시 재고를 되돌려야 한다. `order.fail()`은 `clearAutomatically = true`가 영속성 컨텍스트를 비우기 전에 반영되도록 복구 루프보다 **앞에** 둔다.

**대안으로 고려했던 것 & 그 이유:** 
- Product 행에 비관적 락 추가 후 더티체킹 유지: 정합성은 확보되지만 차감 경로(조건부 UPDATE)와 패턴이 갈리고, 주문 행 락에 상품 행 락이 겹쳐 락 순서 관리 부담이 생긴다.
- 낙관적 락(`@Version`) + 재시도: 만료 배치처럼 경합이 낮은 경로에 재시도 인프라를 얹는 비용이 이득보다 크다.
- `restock_status` 컬럼으로 복구 여부 추적: `findByIdForUpdate` → 상태 체크 → `order.fail()` 순서가 이미 멱등 가드로 동작하므로 스키마 비용 대비 이득이 없다.
- `max_stock` 상한으로 과다 복구 방지: 위와 같은 이유로 불필요하며, 판매자가 정한 재고와 별개의 개념을 도입하게 된다.

**영향받는 문서 / 파일:** 
- `src/main/java/com/example/allinmarket/domain/product/repository/ProductRepository.java`
- `src/main/java/com/example/allinmarket/buyer/order/service/StockReleaseService.java`
- `src/main/java/com/example/allinmarket/domain/product/entity/Product.java`
- `docs/PERSISTENCE.md`, `TD-002`
