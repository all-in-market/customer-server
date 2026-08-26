# Troubleshooting

## TR-001 / 2026-06-05: Flyway migration test fails because the PostgreSQL image has no vector extension

**증상:** 
`./gradlew test --tests '*Migration*'` 또는 `./gradlew test` 실행 시 `FlywayMigrationTest`가 실패한다.

**원인:** 
테스트 컨테이너가 일반 `postgres:16-alpine` 이미지를 사용하면 `V9__add_vector_store.sql`의 `CREATE EXTENSION IF NOT EXISTS vector`를 실행할 수 없다. 해당 이미지에는 `vector.control` 파일이 없어 PostgreSQL이 `extension "vector" is not available` 오류를 반환한다.

**조사 과정:** 
테스트 리포트 `build/test-results/test/TEST-com.example.allinmarket.migration.FlywayMigrationTest.xml`에서 실패 migration이 `V9__add_vector_store.sql`임을 확인했다. 로그에는 PostgreSQL 16 컨테이너가 정상 기동된 뒤 Flyway가 V1부터 V8까지 적용하고, V9에서 `vector` extension 부재로 rollback한 내용이 남아 있었다.

**해결:** 
`FlywayMigrationTest`의 PostgreSQL Testcontainer 이미지를 `pgvector/pgvector:pg16`으로 변경했다. 또한 migration 검증에서 `vector`와 `pg_trgm` extension 및 주요 인덱스 존재 여부를 확인하도록 유지했다.

**재발 방지:** 
vector 검색 기능은 실제 기능이므로 migration 테스트와 운영 DB 모두 pgvector 확장을 지원하는 PostgreSQL 환경을 전제로 해야 한다. 향후 DB 이미지나 RDS/Aurora 구성을 바꿀 때 pgvector 지원 여부를 먼저 확인한다.

**관련 항목:** `TD-001`

## TR-002 / 2026-06-09: k6 scenario-b fails with "No products found" after dummy data seeding

**증상:** 
`k6/scenario-b.js`를 Docker Compose로 실행하면 setup 단계에서 `No products found. Seed products before running test.` 오류가 발생한다. `application-local.yml`에서 dummy data seeding을 활성화했고 DB에는 상품 데이터가 존재하는데도 k6는 상품이 없다고 판단한다.

**원인:** 
상품 더미 데이터가 없는 것이 아니라 Redis에 오래된 빈 상품 목록 캐시가 남아 있었다. `BuyerProductService`는 10페이지 미만 상품 목록을 `products:search:{page}:{size}:{sort}` 키로 10분 캐싱한다. k6 setup 요청인 `/products?page=0&size=20`은 `products:search:0:20:createdAt: ASC` 캐시 키와 일치했고, 이 키에 seeding 전 빈 `PageResponse`가 저장되어 있어 DB 조회 대신 빈 응답이 반환됐다.

**조사 과정:** 
PostgreSQL에서 `products`와 visible products 수를 확인했을 때 각각 `1,000,000`건이 존재했다. k6 컨테이너에서 직접 `/products?page=0&size=20`을 호출하면 `status=200`이지만 `data.content=[]`, `totalElements=0`이 반환됐다. 반면 `/products?page=10&size=20`, `/products?page=0&size=21`, `/products?page=0&size=20&keyword=Shoes`는 상품을 정상 반환했다. Redis에서 `products:search:*` 키를 확인하니 `products:search:0:20:createdAt: ASC`가 존재했다.

**해결:** 
문제 캐시 키를 삭제했다.

```bash
docker compose exec -T redis redis-cli DEL 'products:search:0:20:createdAt: ASC'
```

삭제 후 k6 컨테이너에서 `/products?page=0&size=20`을 다시 호출하자 상품 목록이 정상 반환됐다. 이후 아래 smoke 검증도 통과했다.

```bash
docker compose -f docker-compose-k6.yml run --rm \
  -e TEST_TYPE=smoke \
  -e K6_OUT=influxdb=http://influxdb:8086/k6 \
  k6 run /k6/scenario-b.js
```

**재발 방지:** 
더미 데이터를 재생성하거나 seeding 직후 k6를 실행하기 전 상품 목록 캐시를 비운다. 전체 상품 목록 캐시를 비우려면 아래 명령을 사용한다.

```bash
docker compose exec -T redis redis-cli --scan --pattern 'products:search:*' \
  | xargs -r docker compose exec -T redis redis-cli DEL
```

또한 `scenario-b.js`의 setup 요청이 `page=0&size=20`에 고정되어 있으므로, 동일 오류가 발생하면 먼저 `products:search:0:20:createdAt: ASC` 키를 확인한다.

**관련 항목:** `TD-002`

## TR-003 / 2026-06-10: DataJpaTest fails because Querydsl JPAQueryFactory bean is missing

**증상:** 
`./gradlew test --tests '*BuyerProduct*'` 실행 시 새 `BuyerProductQueryCountTest`가 ApplicationContext를 로드하지 못하고 실패한다. 실패 로그에는 `No qualifying bean of type 'com.querydsl.jpa.impl.JPAQueryFactory' available`가 표시된다.

**원인:** 
`@DataJpaTest`는 JPA repository slice를 구성하면서 `CustomSellerDailyStatisticsRepositoryImpl` 같은 custom repository 구현도 함께 생성한다. 해당 구현은 `JPAQueryFactory`를 생성자 주입으로 요구하지만, 새 테스트에서 `QuerydslConfig`를 import하지 않아 `JPAQueryFactory` bean이 존재하지 않았다.

**조사 과정:** 
테스트 리포트 `build/test-results/test/TEST-com.example.allinmarket.buyer.product.service.BuyerProductQueryCountTest.xml`에서 `UnsatisfiedDependencyException`의 원인이 `JPAQueryFactory` 누락임을 확인했다. 기존 `CustomSellerDailyStatisticsRepositoryImplTest`는 `@Import({QuerydslConfig.class, JpaAuditingConfig.class})`를 사용하고 있어 동일한 JPA slice 테스트 패턴을 참고했다.

**해결:** 
`BuyerProductQueryCountTest`에 `QuerydslConfig`를 import했다.

```java
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
```

수정 후 아래 명령이 통과했다.

```bash
./gradlew test --tests '*BuyerProduct*'
```

**재발 방지:** 
repository slice 테스트에서 custom Querydsl repository가 함께 스캔될 수 있으므로, `@DataJpaTest`가 `JPAQueryFactory`를 요구하는 경우 `QuerydslConfig`를 함께 import한다. auditing 필드가 `nullable=false`인 엔티티를 persist하는 테스트는 `JpaAuditingConfig`도 같이 import한다.

**관련 항목:** `Commit 6`
## TR-004 / 2026-08-25: 아웃박스 스케줄러가 자기 자신이 잡은 락을 기다려 영구 정지한다

**증상:** 
`DashboardOutboxScheduler`를 활성화하면 첫 주기에서 스레드가 멈춘 채 돌아오지 않는다. 예외도 타임아웃 로그도 남지 않고 `Outbox 처리 성공` 로그가 한 건도 찍히지 않는다. 결제는 정상 성공하는데 판매자 대시보드가 갱신되지 않는다. 스케줄러 풀 크기가 기본값 1이므로 이 스레드가 막히는 순간 **다른 6개 스케줄러(주문 만료·통계·정산·지급 등)도 함께 멈춘다.**

`19c8fb1 feat: 스케줄링 처리 정지`(2026-05-10) 커밋으로 `@EnableScheduling`이 주석 처리된 뒤 3개월 이상 스케줄링 전체가 꺼져 있었다. 이 정지의 실질적 원인이 아래 구조로 추정된다.

**원인:** 
호출 경로가 자기 자신을 차단한다.

```
processOutbox()              @Transactional     → findTop100ForUpdate 로 100행에 PESSIMISTIC_WRITE
  └ processSingleEvent()     REQUIRES_NEW       → 외부 트랜잭션 서스펜드
      └ markProcessed()      REQUIRES_NEW       → saveAndFlush
          └ UPDATE dashboard_outbox SET processed = true WHERE id = ?
              ↑ 외부 트랜잭션이 FOR UPDATE 로 잡고 있는 바로 그 행
```

내부 트랜잭션은 외부 트랜잭션의 행 잠금을 기다린다. 그런데 외부 트랜잭션은 **서스펜드된 채 애플리케이션 코드에서 내부 트랜잭션의 리턴을 기다린다.** 이 대기 순환은 DB가 아니라 애플리케이션을 경유하므로 **PostgreSQL 데드락 감지기가 잡지 못한다.** `application.yaml`에 `lock_timeout`도 없어 무한 대기가 된다.

부차적으로 `findTop100ForUpdate`는 `retry_count`를 조건에 쓰지 않아(인덱스 `idx_dashboard_outbox_polling`은 `(processed, retry_count, id)`), 첫 100건이 계속 실패하면 영원히 같은 100건만 재조회하는 헤드 블로킹도 있었다.

**조사 과정:** 
같은 리포의 `HistoryOutboxScheduler`가 동일한 아웃박스 폴링을 하면서도 멈추지 않는다는 점이 실마리였다. 두 경로를 비교하니 `HistoryOutboxScheduler`에는 **스케줄러 레벨 트랜잭션이 없고** `HistoryOutboxService.process(Long id)`가 자신의 `REQUIRES_NEW` 안에서 `findByIdForUpdate`로 직접 잠근다. 즉 락을 잡는 트랜잭션과 UPDATE하는 트랜잭션이 동일하다. 반면 Dashboard 경로는 락을 잡는 트랜잭션(외부)과 UPDATE하는 트랜잭션(내부 REQUIRES_NEW)이 달랐다. **두 아웃박스가 서로 다른 패턴이었던 것이 문제의 뿌리였다.**

`DashboardOutboxStatusService`가 `markProcessed`/`increaseRetry`를 각각 별도 `REQUIRES_NEW`로 감싸고 있어 트랜잭션이 3중으로 중첩되고 있었던 점(Hikari 풀 20에서 건당 커넥션을 2개씩 점유)도 함께 확인했다.

**해결:** 
`HistoryOutbox` 패턴으로 통일했다(TD-004).

1. `DashboardOutboxScheduler`에서 `@Transactional` 제거, `findUnprocessedIds(maxRetryCount, Pageable)`로 **ID만** 조회.
2. `processSingleEvent(Long outboxId)`가 자신의 `REQUIRES_NEW` 안에서 `findByIdForUpdate(id)` + `isProcessed()` 멱등 가드를 수행하고, 성공 시 같은 트랜잭션에서 더티체킹으로 마킹.
3. `DashboardOutboxStatusService` 삭제(중첩 트랜잭션 제거).
4. 조회 조건에 `retryCount < :maxRetryCount`를 넣어 헤드 블로킹 제거. 재시도 소진 건을 `markProcessed`로 성공 위장하던 유실 로직도 삭제.
5. `OrderExpiryScheduler`는 진행 0건이면 루프를 중단하고 `max-loops` 상한을 둔다(기존에는 offset 0 고정 재조회 + 예외 전량 삼킴으로 무한 루프 가능).
6. `@EnableScheduling`을 `SchedulingConfig`로 분리하고 풀 크기를 5로 명시(TD-005).

**재발 방지:** 
- 아웃박스 폴링은 **"ID만 조회 → 건별 `REQUIRES_NEW` 안에서 잠금·처리·마킹"** 한 가지 패턴만 쓴다. 새 아웃박스를 추가할 때 스케줄러에 `@Transactional`을 붙이지 않는다.
- 부모 트랜잭션이 잠근 행을 자식 `REQUIRES_NEW`가 건드리는 구조는 DB 데드락 감지에 걸리지 않는다. `REQUIRES_NEW`를 쓸 때는 **부모가 어떤 행을 잠그고 있는지** 먼저 확인한다.
- 배치 루프에는 항상 반복 상한과 "진행 없으면 중단" 조건을 함께 넣는다. 예외를 삼키는 루프는 그 자체로 무한 루프 후보다.
- 스케줄러 스레드 풀 크기를 기본값(1)으로 두지 않는다. 하나가 막히면 전부 멈춘다.
- 회귀 테스트: `DashboardOutboxSchedulerTest`(건별 격리), `DashboardOutboxServiceTest`(재시도 소진 시 `processed`가 true로 바뀌지 않음), `OrderExpirySchedulerTest`(진행 0건이면 조회 1회로 종료).

**관련 항목:** `TD-004`, `TD-005`
