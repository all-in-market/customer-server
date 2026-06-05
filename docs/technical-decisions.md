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
