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
