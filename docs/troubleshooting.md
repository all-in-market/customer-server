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
