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
