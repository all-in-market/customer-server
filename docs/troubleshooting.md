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
