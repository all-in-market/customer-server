# 인증 · 인가 구조

> **이 문서의 역할**: 보안 필터 체인, 인가 규칙, 토큰/Redis 키 구조와 **새 엔드포인트를 추가할 때 지켜야 할 것**.
> 설계 근거와 대안 비교는 [TRD.md](./TRD.md) §6 및 [README.md](../README.md) §5를 본다.
>
> 관련: [ARCHITECTURE.md](./ARCHITECTURE.md) · [API_REFERENCE.md](./API_REFERENCE.md)

---

## 1. 필터 체인

`common/config/SecurityConfig.java`

```java
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
.addFilterBefore(loginRateLimitFilter, JwtAuthenticationFilter.class)
```

실행 순서는 **`LoginRateLimitFilter` → `JwtAuthenticationFilter` → 인가**다. 속도 제한을 인증 연산보다 앞에 두어 불필요한 BCrypt/DB 조회를 차단한다. 세션은 `STATELESS`, CSRF는 비활성이다.

---

## 2. 인가 규칙 (순서가 중요하다)

| 순서 | 매처 | 정책 |
|---|---|---|
| 1 | `/actuator/**` | `permitAll` |
| 2 | `/auth/logout` | `hasRole("BUYER")` |
| 3 | `/auth/**` | `permitAll` |
| 4 | `/seller/auth/logout` | `hasRole("SELLER")` |
| 5 | `/seller/auth/**` | `permitAll` |
| 6 | `/products/**` | `permitAll` |
| 7 | `GET /categories` | `permitAll` |
| 8 | `/seller/**` | `hasRole("SELLER")` |
| 9 | `/sellers/**` | `hasRole("SELLER")` |
| 10 | **그 외 전부** | `hasRole("BUYER")` |

### ⚠️ 새 엔드포인트를 추가할 때

- **구매자용 API**: 규칙을 추가할 필요가 없다. 캐치올(`anyRequest().hasRole("BUYER")`)이 자동으로 보호한다.
- **공개 API 또는 판매자 전용 API**: 반드시 **캐치올보다 위에** 명시적 규칙을 추가한다. 빠뜨리면 에러 없이 조용히 구매자 전용이 되어, 판매자/비로그인 요청이 403으로 실패한다.
- 로그아웃처럼 "경로는 공개 그룹인데 인증이 필요한" 케이스는 그룹 규칙보다 **먼저** 선언한다(위 표 2·4번).

---

## 3. 역할과 승인 상태

| 개념 | 위치 | 값 |
|---|---|---|
| 역할 | `common/enums/UserRole` | `BUYER`, `SELLER` (DB CHECK 제약에는 `ADMIN`도 포함) |
| 판매자 승인 | `seller/enums/SellerStatus` | `PENDING`(가입 직후) / `APPROVED` / `REJECTED` |

**승인 검사는 인가 규칙이 아니라 로그인 시점에 한다.** `SellerAuthService`가 `status != APPROVED`면 로그인을 거부하므로, 미승인 판매자는 토큰 자체를 발급받지 못한다. 배치(정산 등)도 `SellerRepository.findAllActiveSellers()`로 `deletedAt IS NULL AND status = APPROVED`인 판매자만 대상으로 한다.

---

## 4. 토큰 구조

| 항목 | 값 |
|---|---|
| Access Token | JWT (jjwt 0.12.6). 만료 `jwt.expiration`, 기본 3,600,000ms(1시간) |
| Refresh Token | **Opaque UUID** — JWT가 아니다. Redis에 저장 |
| Refresh 전달 | 응답 바디가 아닌 `HttpOnly` + `Secure` + `SameSite=Strict` 쿠키 |
| 발급/검증 | `common/security/JwtProvider` |
| 현재 사용자 조회 | `common/security/SecurityUtils.getCurrentUserId()` |

### 4.1 Refresh Token Rotation (RTR)

`buyer/auth/service/BuyerAuthService` (판매자도 동일 패턴)

```
로그인   : refresh:{role}:{token} = userId  (TTL 7일)
          user_refreshes:{role}:{userId} SADD token  (TTL 7일)
재발급   : getAndDelete("refresh:{role}:{token}")   ← 조회+삭제를 원자적으로
          → 없으면 이미 사용된 토큰 = 탈취 의심 → 거부
          새 토큰 발급 후 Set에서 교체
로그아웃 : role claim이 기대 역할과 다르면 FORBIDDEN으로 거부
          user_refreshes:{role}:{userId} 전체 조회 → refresh:{role}:{token} 일괄 삭제
          Access Token은 blacklist:{token} 등록 (TTL = 토큰 잔여 시간)
```

한 번 쓴 refresh token이 다시 들어오면 그 자체가 침해 신호다. 이 동작을 바꾸지 않는다.

### 4.2 Redis 키 목록

| 키 | 용도 | TTL |
|---|---|---|
| `refresh:buyer:{token}` / `refresh:seller:{token}` | refresh token → userId. **페르소나별로 키 공간이 분리된다** | 7일 |
| `user_refreshes:buyer:{userId}` / `user_refreshes:seller:{userId}` | 사용자의 활성 refresh token Set (멀티 디바이스 로그아웃) | 7일 |
| ~~`refresh:{token}`~~ (구 형식) | role 구분자가 없는 레거시 키. **값을 읽지 않고 삭제만 한 뒤 거부**한다 | 7일 후 소멸 |
| `blacklist:{token}` | 로그아웃된 access token | 토큰 잔여 시간 |
| `login:fail:ip-email:{ip}:{sha256(email)}` | 로그인 실패 카운터 | 300초 |
| `login:fail:email:{sha256(email)}` | 로그인 실패 카운터 | 300초 |

**장애 정책**: Redis 장애 시 인증은 **fail-closed**로 동작한다(우회 허용 대신 `REDIS_UNAVAILABLE` 503). 이 정책을 fail-open으로 바꾸지 않는다.

**왜 키에 role 세그먼트가 있는가**: `buyers`와 `sellers`는 각각 독립 IDENTITY 시퀀스(`V1__init.sql`)라 **ID가 반드시 겹친다.** 키에 role이 없으면 저장 값(userId)만으로는 페르소나를 구분할 수 없어, 판매자 refresh token으로 `POST /auth/refresh`를 호출하면 같은 ID의 **다른 구매자 계정 access token이 발급**된다. 로그아웃도 동일 ID 타 페르소나의 세션을 지운다. 키를 만들 때는 문자열을 직접 잇지 말고 `common/auth/consts/AuthConsts`의 `refreshKey(role, token)` / `userRefreshesKey(role, userId)`를 쓴다.

**서비스 계층 이중 방어**: `logout()`은 `JwtProvider.getRole()`로 access token의 role claim이 기대 역할과 일치하는지 검사하고 다르면 `FORBIDDEN`을 던진다. 인가 규칙(§2)이 다시 어긋나도 서비스가 막는다.

---

## 5. 로그인 속도 제한

`common/security/LoginRateLimitFilter.java`

| 상수 | 값 | 의미 |
|---|---|---|
| `LOGIN_PATHS` | `POST /auth/login`, `POST /seller/auth/login` | 이 경로만 검사 |
| `MAX_FAILURES_IP_EMAIL` | 5 | IP+계정 조합 실패 허용 횟수 |
| `MAX_FAILURES_EMAIL` | 10 | 계정 단독 실패 허용 횟수 (IP 분산 공격 방어) |
| `BLOCK_DURATION_SECONDS` | 300 | 차단 및 카운터 유지 시간 |
| `MAX_BODY_BYTES` | 8,192 | 초과 요청 차단 (메모리 고갈 방지) |

- **이메일은 SHA-256 해싱** 후 키로 쓴다. Redis에 평문 이메일을 저장하지 않는다.
- 클라이언트 IP는 `X-Forwarded-For`의 **마지막** 값을 신뢰한다(ALB 뒤이므로 앞쪽 값은 스푸핑 가능).
- 차단 시 `ErrorEnum.LOGIN_RATE_LIMITED`로 응답한다.

---

## 6. 계정 열거 · 타이밍 공격 방어

- 로그인 실패 사유(계정 없음 / 비밀번호 불일치)를 **구분해서 응답하지 않는다.** 통일된 메시지만 반환하고, 상세 사유는 서버 로그에만 남긴다.
- 존재하지 않는 이메일에도 더미 BCrypt 연산을 수행해 응답 시간 차이로 계정 존재 여부를 유추하지 못하게 한다.

인증 관련 코드를 수정할 때 이 두 성질이 깨지지 않는지 확인한다.

---

## 7. 서버 간 인증 (HMAC)

외부 알림 서버 호출은 `common/security/HmacSigner`로 서명한다.

```java
String signature = HmacSigner.sign(secret, timestamp + requestId + body);
```

| 설정 | 환경 변수 |
|---|---|
| `notification.auth.client-id` | `CUSTOMER_SERVER` |
| `notification.auth.secret` | `SERVER_SECRET_KEY` |
| `notification-server.url` | 프로파일별 (prod: `https://hyu1335.cloud`) |

`timestamp`는 epoch seconds, `requestId`는 UUID다. 서명 규격을 바꾸면 **알림 서버와 동시에 배포**해야 한다.

---

## 8. 테스트에서의 인증

- 컨트롤러 슬라이스 테스트는 `@WebMvcTest` + `RestTestClient`를 쓰고, `spring-security-test`의 `@WithMockUser` 계열로 역할을 부여한다.
- 필터 체인 전체를 검증해야 하는 경우가 아니면 `SecurityUtils`가 반환할 사용자 ID를 기준으로 서비스 계층을 검증하는 편이 빠르다.

---

## 9. 알려진 제약

| 항목 | 내용 |
|---|---|
| `/actuator/**` 전체 공개 | `metrics`, `prometheus`까지 인증 없이 노출된다. health와 분리하거나 내부망/ALB로 제한하는 것이 개선 과제다([TRD.md](./TRD.md) §14) |
| WAF | 대량 요청 1차 차단은 AWS WAF 운영 설정에 의존하며, 이 리포지토리의 Terraform 코드에는 포함되어 있지 않다 |
