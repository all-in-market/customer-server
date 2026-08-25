# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`all-in-market` customer-server: 멀티 벤더 커머스 플랫폼의 구매자/판매자 백엔드.
Spring Boot 4.0.5 / Java 21 모놀리식 서버로, 상품·장바구니·주문·결제·환불·재입고 알림·판매자 대시보드/통계·정산/지급을 담당한다.
채팅, 알림 발송, AI 챗봇, 관리자 기능은 같은 DB를 공유하는 **별도 서버**의 몫이다(이 리포지토리에는 해당 테이블 마이그레이션만 있다).

## Commands

```bash
./gradlew build          # Windows: gradlew.bat build
./gradlew bootRun        # Postgres + Redis + 환경변수 필요
./gradlew test
./gradlew test --tests "com.example.allinmarket.buyer.order.service.OrderServiceTest"
./gradlew test --tests "com.example.allinmarket.buyer.order.service.OrderServiceTest.methodName"
./gradlew asciidoctor    # REST Docs 생성 (test 이후 실행)
docker compose up -d     # Postgres(pgvector), Redis, RedisInsight, pgAdmin
docker compose -f docker-compose-k6.yml up --abort-on-container-exit   # 부하 테스트
```

- `asciidoctor`는 `test` 이후, `bootJar`는 `asciidoctor` 이후 실행된다 → **jar 빌드는 항상 전체 테스트를 재실행**하고 문서를 `static/docs`로 번들한다.
- 필수 환경변수(`.env`, 커밋 안 됨): `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `SELLER_ID`, `SELLER_PASSWORD`, `CUSTOMER_SERVER`, `SERVER_SECRET_KEY`, `PGADMIN_EMAIL`, `PGADMIN_PASSWORD`.
- 프로파일: `application.yaml`(공통) + `application-{local,dev,prod}.yml`.

## 상세 문서 (작업 전에 해당 문서를 먼저 읽는다)

| 무엇을 건드리는가 | 읽을 문서 |
|---|---|
| 코드를 어디에 놓을지, 패키지 경계, 스케줄러, 외부 연동 | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| 엔드포인트, 응답/에러 규약, DTO 규칙, REST Docs | [docs/API_REFERENCE.md](docs/API_REFERENCE.md) |
| 인증 필터, 인가 규칙, 토큰·Redis 키, rate limit | [docs/AUTH.md](docs/AUTH.md) |
| 마이그레이션, DB 제약, 락·트랜잭션, 아웃박스, 캐시 | [docs/PERSISTENCE.md](docs/PERSISTENCE.md) |
| 왜 이렇게 설계했는가 (요구사항·근거) | [docs/PRD.md](docs/PRD.md), [docs/TRD.md](docs/TRD.md) |
| 네이밍·DTO·검증 등 코드 컨벤션 전문 | [docs/rules/CODE_CONVENTION.md](docs/rules/CODE_CONVENTION.md) |
| 브랜치·커밋·PR 규칙 | [docs/rules/GITHUB_RULES.md](docs/rules/GITHUB_RULES.md) |
| 과거 의사결정 / 장애 기록 | [docs/technical-decisions.md](docs/technical-decisions.md), [docs/troubleshooting.md](docs/troubleshooting.md) |
| 코드베이스 리뷰 진단 (회차별) | [docs/feedback/](docs/feedback/README.md) |

## 작업 원칙

### 변경 범위

- 작고 리뷰 가능한 단위로 바꾼다. 명시적 요청 없이 프로젝트 전반을 재작성하지 않는다.
- PR 가이드라인: 10개 파일 / 400 diff 라인 이하, 커밋 하나에 논리 변경 하나.
- 커밋 타입: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `release`. 제목 50자 이하.
- 브랜치: `main` ← `dev` ← `feature/{domain-method}/{description}`.

### 코드를 쓸 때

- **패키지 경계를 지킨다**: `domain.*`에는 엔티티/리포지토리만, 비즈니스 로직은 `buyer.*` / `seller.*`, 횡단 관심사는 `common.*`.
- **응답은 항상 `ApiResponse<T>`**, 페이지는 `PageResponse<T>`. 예외는 `BaseException(ErrorEnum)`을 던지고 `GlobalExceptionHandler`가 처리한다. 컨트롤러에서 임의 메시지를 만들지 않는다.
- **DTO는 `record` + 정적 팩토리 `from(entity)`**. 요청은 `XxxCreateRequest`/`XxxUpdateRequest`, 응답은 `XxxDetailResponse`(다건은 `List<...>`). 패키지는 `dto/request` · `dto/response`로 나눈다.
- **클래스 접미사 필수**: `XxxController`, `XxxService`, `XxxRepository`. 조회 메서드는 `findXBy...`/`existsBy...`, 명령 메서드는 `verbDomain()`. 컨트롤러와 서비스의 메서드명은 가급적 일치시킨다.
- **서비스는 클래스 상단에 `@Transactional(readOnly = true)`**, 쓰기 메서드에만 `@Transactional`을 다시 선언한다.
- **Create 엔드포인트는 `ResponseEntity.status(HttpStatus.CREATED)`**, 그 외는 `ResponseEntity.ok()`.
- 상수는 도메인별 `consts` 패키지의 `final` 클래스(+ private 생성자), enum은 `enums` 패키지.
- `facade`는 같은 레이어 상호 의존이 불가피할 때만. 임의로 추가하지 말고 이슈로 논의한다.
- 금액은 0을 허용한다 → `@Positive`가 아니라 `@PositiveOrZero`.
- 문자열 길이 제한은 `@Size`. 길이 제한이 없는 DTO 문자열도 255자, 비밀번호는 20자로 막는다.
- 그래프·시각화 자료는 README에 직접 넣지 않고 `docs/improvement/`에서 관리한다.

### 특히 조심할 것

1. **새 엔드포인트의 인가**: `SecurityConfig`의 캐치올이 `anyRequest().hasRole("BUYER")`다. 구매자 API는 추가 설정이 필요 없지만, **공개 API나 판매자 전용 API는 캐치올보다 위에 명시적 규칙을 추가**해야 한다. 빠뜨리면 조용히 구매자 전용이 된다. → [docs/AUTH.md](docs/AUTH.md)
2. **스키마 변경**: `ddl-auto: validate`다. 엔티티만 고치면 기동에 실패한다. Flyway 마이그레이션을 **반드시 함께** 추가한다. → [docs/PERSISTENCE.md](docs/PERSISTENCE.md)
3. **"하나만 존재해야 하는" 규칙**: 서비스 검증만으로는 동시 요청에서 깨진다. 유니크(또는 부분 유니크) 인덱스를 함께 건다.
4. **재고 차감 경로**: 상품 ID 정렬 후 락 획득 → 조건부 원자 UPDATE의 row count로 판정. 엔티티 setter 방식으로 바꾸지 않는다.
5. **자가 호출 금지**: 같은 클래스 안에서 `this.method()`를 호출하면 `@Transactional(REQUIRES_NEW)`, `@Async`, `@Retryable`이 조용히 무시된다. 전파 속성이 다른 로직은 별도 빈으로 분리한다.
6. **외부 호출·캐시 갱신은 커밋 이후에**: `@TransactionalEventListener(AFTER_COMMIT)` 또는 `afterCommit()` 훅을 쓴다. 트랜잭션 안에서 직접 호출하지 않는다.
7. **스케줄러는 모든 ECS 인스턴스에서 동시에 돈다**: 새 배치에는 비관적 락이나 유니크 제약으로 중복 처리 방어를 넣는다.
8. **결제 트랜잭션에 무거운 로직을 붙이지 않는다**: 부수효과는 아웃박스(`dashboard_outbox`, `history_outboxes`)에 적재한다.

### 테스트

- 단위 테스트는 **서비스 레이어부터** 작성한다.
- 새 컨트롤러 테스트는 `@WebMvcTest` + `@AutoConfigureRestTestClient` + `RestTestClient`, 의존성은 `@MockitoBean`(제거된 `@MockBean` 아님)으로 쓴다.
  `RestDocsControllerTest`(MockMvc 기반, `src/test/.../support`)는 **레거시**다. 복제하지 않는다.
- 컨트롤러 테스트는 REST Docs 소스를 겸한다. API를 추가/변경하면 스니펫과 `docs/asciidoc/**`, [docs/API_REFERENCE.md](docs/API_REFERENCE.md)를 함께 갱신한다.
- 마이그레이션 테스트의 Testcontainers 이미지는 `pgvector/pgvector:pg16`이어야 한다(`V9`의 `CREATE EXTENSION vector` 때문).
- 코드 변경 후 가능하면 `./gradlew test`를 돌린다.

### 문서화

- 영속성 변경 시 트랜잭션 경계, 인덱스, N+1 위험, 마이그레이션 영향을 확인하고 기록한다.
- 새로운 기술 선택은 [docs/technical-decisions.md](docs/technical-decisions.md)에, 장애 대응은 [docs/troubleshooting.md](docs/troubleshooting.md)에 템플릿(`docs/templates/`) 형식으로 남긴다.
- 코드베이스 전반을 훑은 리뷰 진단은 `docs/feedback/피드백{N}차.md`에 회차별로 남긴다. 기존 회차 파일은 수정하지 않는다 → [docs/feedback/README.md](docs/feedback/README.md).
- 포트폴리오용 서술은 문제 → 원인 → 해결 → 결과 → 배운 점 순서로 쓴다.

## 그 밖

- Harness skill source of truth: `.agents/skills/harness/SKILL.md` (`.codex/skills/harness/SKILL.md`는 포워딩 스텁).
- 인프라/CI 셋업: [INFRA_CICD_SETUP_GUIDE.md](INFRA_CICD_SETUP_GUIDE.md), Terraform은 `infra/`, 워크플로는 `.github/workflows/`.
