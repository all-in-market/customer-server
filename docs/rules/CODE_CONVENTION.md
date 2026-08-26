# 코드 컨벤션

> 이 문서는 **어떻게 쓸 것인가**(네이밍·DTO·검증·응답 규약)를 다룬다.
> **어디에 놓을 것인가**는 [ARCHITECTURE.md](../ARCHITECTURE.md), 엔드포인트 규약은 [API_REFERENCE.md](../API_REFERENCE.md), 브랜치·커밋은 [GITHUB_RULES.md](./GITHUB_RULES.md)를 본다.
>
> 아래 코드 예시는 **실제 구현과 일치**한다. 구현을 바꾸면 이 문서도 함께 고친다.

---

## 시각화

- 그래프 등의 시각화 자료는 **README** 에 바로 추가하는 것이 아닌 별도로 ***docs/improvement*** 디렉터리를 만들어 관리한다.
- 기존 `README.md`의 mermaid 다이어그램과 `docs/image/APIServerERD.png`는 이 규칙 이전에 작성된 것으로 현행 유지한다. **새로 추가하는** 시각화만 `docs/improvement/`에 둔다.

---

## 공통 응답 처리

### ApiResponse

모든 컨트롤러 응답은 `ApiResponse<T>`로 감싼다. 컨트롤러에서 임의 메시지를 만들지 않고 `SuccessEnum` / `ErrorEnum`을 통한다.

```java
@JsonPropertyOrder({"success", "status", "message", "data", "timestamp"})
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        LocalDateTime timestamp,
        T data
) {
    public static <T> ApiResponse<T> success(SuccessEnum successEnum, T data) {
        return new ApiResponse<>(true, successEnum.getStatus(), successEnum.getMessage(), LocalDateTime.now(), data);
    }

    public static ApiResponse<Void> fail(ErrorEnum errorEnum) {
        return new ApiResponse<>(false, errorEnum.getStatus(), errorEnum.getMessage(), LocalDateTime.now(), null);
    }

    // 검증 실패처럼 상수 메시지 대신 상세 메시지를 내보내야 할 때만 사용
    public static ApiResponse<Void> fail(ErrorEnum errorEnum, String message) {
        return new ApiResponse<>(false, errorEnum.getStatus(), message, LocalDateTime.now(), null);
    }
}
```

- `status`는 **HTTP 상태 코드(int)** 다. 문자열 코드가 아니다.
- `timestamp`는 record 컴포넌트에 기본값을 줄 수 없으므로(자바 문법상 불가) **정적 팩토리에서 `LocalDateTime.now()`를 주입**한다.

### SuccessEnum / ErrorEnum

실제 상수 목록은 코드가 단일 출처다 → `common/enums/SuccessEnum.java`, `common/enums/ErrorEnum.java`.

```java
@Getter
@AllArgsConstructor
public enum SuccessEnum {

    // Auth
    REGISTER_SUCCESS(201, "회원가입에 성공하였습니다."),
    LOGIN_SUCCESS(200, "로그인에 성공하였습니다."),

    // 성공한 경우 별도로 구분하지 않음
    CREATE_SUCCESS(201, "데이터 생성에 성공하였습니다."),
    READ_SUCCESS(200, "데이터 조회에 성공하였습니다."),
    UPDATE_SUCCESS(200, "데이터 수정에 성공하였습니다."),
    DELETE_SUCCESS(200, "데이터 삭제에 성공하였습니다.");

    private final int status;
    private final String message;
}
```

추가할 때 지키는 것:

- 첫 인자는 **HTTP 상태 코드(int)**, 두 번째는 사용자에게 그대로 노출되는 한국어 메시지.
- 도메인별로 `// Order`, `// Payment`처럼 주석 그룹을 지어 그 안에 넣는다. 공통 항목은 `// Common` 그룹.
- 성공 응답은 도메인마다 새로 만들지 말고 `CREATE_SUCCESS` / `READ_SUCCESS` / `UPDATE_SUCCESS` / `DELETE_SUCCESS`를 재사용한다. 인증처럼 의미가 분명히 다른 경우만 전용 상수를 만든다.

---

### PageResponse

페이지 응답은 `Page<T>`를 그대로 내보내지 않고 `PageResponse<T>`로 감싼다.

```java
@JsonPropertyOrder({"content", "currentPage", "totalPages", "totalElements", "size", "isLast"})
public record PageResponse<T>(
        List<T> content,
        int currentPage,
        int totalPages,
        long totalElements,
        int size,
        boolean isLast
) {
    public static <T> PageResponse<T> register(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber() + 1,   // 응답의 currentPage는 1부터 시작한다
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.isLast()
        );
    }
}
```

- 요청 파라미터(`Pageable`)는 0-based, **응답의 `currentPage`는 1-based**다. 여기서 `+ 1`을 흡수하므로 컨트롤러/서비스에서 다시 변환하지 않는다.

---

## 공통 예외 처리

- ***BaseException*** 을 만들어서 관리한다. 도메인 예외는 모두 이 클래스를 `ErrorEnum`과 함께 던진다.

```java
@Getter
public class BaseException extends RuntimeException {

    private final ErrorEnum errorEnum;

    public BaseException(ErrorEnum errorEnum) {
        super(errorEnum.getMessage());
        this.errorEnum = errorEnum;
    }
}
```

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        log.warn("비즈니스 예외 발생 : {}", e.getMessage());

        ErrorEnum errorEnum = e.getErrorEnum();
        return ResponseEntity
                .status(errorEnum.getStatus())
                .body(ApiResponse.fail(errorEnum));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(ErrorEnum.INVALID_INPUT.getMessage());

        log.error("MethodArgumentNotValidException 발생: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorEnum.INVALID_ARGUMENT, message));
    }

    // ... 이하 동일한 형태
}
```

현재 등록된 핸들러(`common/config/GlobalExceptionHandler.java`):

| 예외 | 응답 |
|---|---|
| `BaseException` | `ErrorEnum`의 상태/메시지 그대로 |
| `MethodArgumentNotValidException` | 400 + **첫 번째 필드 에러 메시지** (`INVALID_ARGUMENT`) |
| `HttpMessageNotReadableException` | 400 `INVALID_INPUT` |
| `ConstraintViolationException` | 400 `INVALID_INPUT` (파라미터 검증) |
| `MethodArgumentTypeMismatchException` | 400 `INVALID_INPUT` (enum·타입 변환 실패) |
| `DataIntegrityViolationException` | 유니크 충돌이면 409 `DATA_CONFLICT`, 그 외 400 `INVALID_INPUT` |
| `Exception` | 500 `INTERNAL_SERVER_ERROR` |

- 새 예외 타입을 잡아야 하면 여기에 핸들러를 추가한다. **컨트롤러에서 try/catch로 응답을 만들지 않는다.**
- 검증 메시지는 `@NotBlank(message = "...")`처럼 DTO 애노테이션에 쓰면 그대로 응답에 실린다.

---

## DTO 클래스

- ***정적 팩토리 패턴*** 을 사용한다 (`from(entity)`).
- `record` 형태로 통일한다.
- String 길이 제한은 `@Size`를 권장한다 (jakarta.validation 표준).
- request, response는 패키지별로 관리한다.
    - 예) `seller/auth/dto/request`, `seller/auth/dto/response`

```java
public record MenuDetailResponse(
        Long id,
        String name,
        Long price
) {
    public static MenuDetailResponse from(Menu menu) {
        return new MenuDetailResponse(
                menu.getId(),
                menu.getName(),
                menu.getPrice()
        );
    }
}
```

> 기존 예외: `buyer/cartitem/dto`, `buyer/category/dto`, `buyer/payment/client/dto`처럼 DTO가 하나뿐인 곳은 `request`/`response` 하위 패키지 없이 평평하다. **새 DTO를 추가할 때는 그 패키지도 `request`/`response`로 나눈다.**

---

## 클래스 네이밍

```
// 레이어별 suffix 필수
SellerProductController
SellerService
SellerServiceImpl     // 구현체 (인터페이스 사용 시)
SellerRepository
Seller                // Entity
SellerValidationConsts

// 요청 dto
MenuCreateRequest
MenuUpdateRequest

// 응답 dto
MenuDetailResponse -> 하나로 통일, 다건 조회는 List<>로 감싸기
```

- 구매자/판매자 API는 **페르소나 접두사**를 붙인다: `BuyerOrderService`, `SellerProductController`.

---

## 메서드 네이밍

```
// 조회: 동사 - 형용사 - 메인 - 조건
findVendorById()
findAllActiveVendors()
existsByBusinessNumber()

// 명령: 동사 - 도메인
registerVendor()
approveVendor()
suspendVendor()

// 이벤트 발행 (Kafka 등): 동사 - 도메인 - 형용사 - 이벤트
publishVendorApprovedEvent()

// 컨트롤러 메서드명과 서비스 메서드명은 가급적 일치시킬 것
BuyerAddressController.createAddress() -> BuyerAddressService.createAddress()
```

---

## Const

- 상수의 경우 도메인별로 `consts` 패키지를 두어 관리한다.
- 클래스를 ***final*** 로 선언하여 상속받지 아니한다.
- Enum은 `enums` 패키지에서 관리한다.

```java
public final class UserConsts { // 도메인-Consts

    private UserConsts() {} // 인스턴스화 방지

    public static final double PI = 3.14;
    public static final double AVOGADROS_NUMBER = 6.022_140_857e23;

}
```

> 기존 예외: `BuyerConsts` / `SellerConsts`에는 private 생성자가 없고, `SettlementConst`는 단수형이다. **신규 클래스는 위 규칙(`XxxConsts` + private 생성자)을 따른다.**

---

## Facade 패턴 도입 여부

- 같은 계층의 클래스 간 의존성이 부득이 하게 생길 때 의존성의 정도에 따라 facade 패턴의 도입 여부를 결정한다 → Issue 생성
- 현재 유일한 사례와 그 근거는 [ARCHITECTURE.md §3.1](../ARCHITECTURE.md)을 본다.

---

## Validation

엔티티와 DTO는 역할이 다르다. **엔티티는 DB 스키마를, DTO는 요청값을** 방어한다.

### 엔티티에 적용

- `@Column(nullable = false)`
- `@Column(precision, scale)`, `@Column(length)`
- (필요시) `@Enumerated`

```java
@PositiveOrZero
@Column(name = "settlement_amount", nullable = false, precision = 12, scale = 2)
private BigDecimal settlementAmount;

@NotBlank // 공백 방어
@Column(nullable = false, length = 100, unique = true)
private String email;
```

### DTO에 적용

- `@NotBlank`, `@NotNull`
- `@Positive`, `@PositiveOrZero`, `@Min`, `@Max`, `@Digits`, `@Size` 등

```java
@NotNull
@PositiveOrZero          // 또는 @DecimalMin(value = "0.0", inclusive = false)
@Digits(integer = 10, fraction = 2)
private BigDecimal amount;
```

규칙:

- 금액은 **0을 허용**한다 → `@Positive`가 아니라 `@PositiveOrZero`.
- `@Column(precision = 12, scale = 2)`은 엔티티에, `@Digits(integer = 10, fraction = 2)`는 DTO에 적용한다. 역할이 다르다.
- 문자열은 `@NotBlank` 사용 (Null + 공백 미허가).
- `@Positive`, `@PositiveOrZero`, `@Negative`, `@Min`, `@Max`, `@DecimalMin`, `@DecimalMax`는 `@NotNull`과 함께 사용한다 (엔티티에서는 `nullable = false` 옵션으로 방어).
- 비밀번호는 길이 20자로 제한한다.
- 길이 제한이 따로 없는 문자열 필드도 DTO에서는 **255자 제한**을 건다.

---

## Service

- 클래스 상단에 `@Transactional(readOnly = true)`를 사용한다.
- 쓰기 메서드에만 `@Transactional`을 다시 선언한다. 클래스에 읽기 메서드가 전혀 없다면 클래스 레벨에 `@Transactional`을 쓴다.

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerAddressService {

    @Transactional
    public AddressDetailResponse createAddress(Long currentUserId, AddressCreateRequest request) { ... }

    public List<AddressDetailResponse> getAllAddresses(Long currentUserId) { ... }
}
```

- **자가 호출 금지**: 같은 클래스 안에서 `this.method()`로 호출하면 프록시를 거치지 않아 `REQUIRES_NEW`, `@Async`, `@Retryable`이 조용히 무시된다. 전파 속성이 다른 로직은 별도 빈으로 분리한다 ([ARCHITECTURE.md §2.2](../ARCHITECTURE.md)).
- 외부 호출·캐시 갱신은 트랜잭션 커밋 이후(`@TransactionalEventListener(AFTER_COMMIT)`)에 하거나 아웃박스에 적재한다.

---

## Test

- **Service 레이어부터 단위 테스트를 작성**한다.
- 컨트롤러 테스트는 `@WebMvcTest` + `@AutoConfigureRestTestClient` + `RestTestClient`, 의존성은 `@MockitoBean`으로 쓴다. `RestDocsControllerTest`(MockMvc 기반)는 레거시이므로 복제하지 않는다.
- 컨트롤러 테스트는 REST Docs 스니펫 소스를 겸한다. API를 추가/변경하면 `docs/asciidoc/**`와 [API_REFERENCE.md](../API_REFERENCE.md)를 함께 갱신한다.

---

## Controller Convention

`HttpStatus.CREATED`

- API의 Create 메서드는 `ResponseEntity.status(HttpStatus.CREATED)`를 사용한다.
- 이 외의 경우는 `ResponseEntity.ok()`를 사용한다.

```java
@PostMapping
public ResponseEntity<ApiResponse<AddressDetailResponse>> createAddress(
        @RequestBody @Valid AddressCreateRequest request
) {
    AddressDetailResponse result = buyerAddressService.createAddress(SecurityUtils.getCurrentUserId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(SuccessEnum.CREATE_SUCCESS, result)
    );
}
```

- 반환 타입은 항상 `ResponseEntity<ApiResponse<T>>`다.
- 인증 주체는 파라미터로 받지 않고 `SecurityUtils.getCurrentUserId()`로 꺼낸다.
