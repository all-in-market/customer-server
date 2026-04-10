## 시각화

- 그래프 등의 시각화 자료는 **README** 에 바로 추가하는 것이 아닌 별도로 ***docs/improvement*** 디렉터리를 만들어 관리

---

## 공통 응답 처리

### ApiResponse

```java
@JsonPropertyOrder({"success", "status", "message", "data", "timestamp"})
public record ApiResponse<T> (
        boolean success,
        String status,
        String message,
        LocalDateTime timestamp,
        T data
) {
    public static <T> ApiResponse<T> success(SuccessEnum successEnum, T data) {
        return new ApiResponse<>(true, successEnum.status, successEnum.message, LocalDateTime.now(), data);
    }

    public static ApiResponse<Void> fail(ErrorEnum errorEnum) {
        return new ApiResponse<>(false, errorEnum.status, errorEnum.message, LocalDateTime.now(), null);
    }
}
```

```java
@Getter
@AllArgsConstructor
public enum SuccessEnum {
    REGISTER_SUCCESS(201, "회원가입에 성공하였습니다."),
    LOGIN_SUCCESS(200, "로그인에 성공하였습니다."),
    LOGOUT_SUCCESS(200, "로그아웃에 성공하였습니다."),
    CREATE_SUCCESS(201, "데이터 생성에 성공하였습니다."),
    READ_SUCCESS(200, "데이터 조회에 성공하였습니다."),
    UPDATE_SUCCESS(200, "데이터 수정에 성공하였습니다."),
    DELETE_SUCCESS(200, "데이터 삭제에 성공하였습니다."),
    CHARGE_SUCCESS(200, "충전에 성공하였습니다.");

    private final int httpStatus;
    private final String message;
}
```

```java
@Getter
@RequiredArgsConstructor
public enum ErrorEnum {

    // Common
    INVALID_INPUT(400, "잘못된 입력값입니다."),
    UNAUTHORIZED(401, "인증이 필요합니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    NOT_FOUND(404, "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다."),
    LOCK_ACQUISITION_FAILED(500, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    REDIS_UNAVAILABLE(503, "Redis 서버에 연결할 수 없습니다."),

    // Menu
    MENU_NOT_FOUND(404, "존재하지 않는 메뉴입니다."),
    MENU_ALREADY_DELETED(400, "삭제된 메뉴입니다."),

    // Order
    ORDER_NOT_FOUND(404, "존재하지 않는 주문입니다."),
    ORDER_ALREADY_COMPLETED(400, "이미 결제 완료된 주문입니다."),
    ORDER_NOT_CANCELLABLE(400, "취소할 수 없는 주문 상태입니다."),

    // Point
    INSUFFICIENT_POINT(400, "포인트 잔액이 부족합니다."),
    POINT_NOT_FOUND(404, "포인트 정보를 찾을 수 없습니다."),

    //Payment
    PAYMENT_ALREADY_EXISTS(400, "이미 결제된 주문입니다."),
    PAYMENT_FAILED(500, "결제 처리 중 오류가 발생했습니다."),
    PAYMENT_NOT_FOUND(404, "결제 내역이 없습니다."),

    // User
    USER_NOT_FOUND(404, "존재하지 않는 사용자입니다."),
    USER_ALREADY_DELETED(400, "이미 탈퇴한 사용자입니다."),
    EMAIL_ALREADY_EXISTS(400, "이미 사용 중인 이메일입니다."),
    PASSWORD_MISMATCH(401, "이메일 또는 비밀번호가 올바르지 않습니다."),

    // Token
    TOKEN_EXPIRED(401, "만료된 토큰입니다."),
    TOKEN_INVALID(401, "유효하지 않은 토큰입니다.");

    private final int status;
    private final String message;
}
```

---

### PageResponse

```java
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
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.isLast()
        );
    }
}
```

---

## 공통 예외 처리

- ***BaseException*** 을 만들어서 관리

```java
@Getter
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
```

---

## DTO 클래스

- ***정적 팩토리 패턴*** 을 사용
- record 형태로 통일

```java
public record MenuResponse(
	Long id,
	String name,
	Long price
	) { 
		public static MenuResponse from(Menu menu) {
			return new MenuResponse(
				menu.getId(),
				menu.getName(),
				menu.getPrice()
				);
		}
}
```

---

## 클래스 네이밍

```
// 레이어별 suffix 필수
VendorController
VendorService         // 인터페이스
VendorServiceImpl     // 구현체 (인터페이스 사용 시)
VendorRepository
Vendor          // Entity

// 요청 dto
MenuCreateRequest
MenuUpdateRequest

// 응답 dto 
MenuDetailResponse -> 하나로 통일, 다건 조회는 List<>로 감싸기 
```

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
```

---

## Const

- 상수의 경우 도메인별로 const 패키지를 두어 관리
- 클래스를 ***final*** 로 선언하여 상속받지 아니함
- Enum은 enum 패키지에서 관리

```java
public final class UserConstants { // 도메인-Constans

    private UserConstants() {} // 인스턴스화 방지

    public static final double PI = 3.14;
    public static final double AVOGADROS_NUMBER = 6.022_140_857e23;

}
```

---

## Facade 패턴 도입 여부

- 같은 계층의 클래스 간 의존성이 부득이 하게 생길 때 의존성의 정도에 따라 facade 패턴의 도입 여부를 결정한다 → Issue 생성

---

## Validation

### DTO에 적용
```
@NotNull
@PositiveOrZero          // 또는 @DecimalMin(value = "0.0", inclusive = false)
@Digits(integer = 10, fraction = 2)
```
- `@Positive`, `@PositiveOrZero`, `@Negative`, `@Min`, `@Max`, `@DecimalMin`, `@DecimalMax` 은 @NotNull과 함께 사용한다. (엔티티에서는 nullable = false 옵션으로 방어)


### 엔티티에 적용
```
@PositiveOrZero
@Column(name = "settlement_amount", nullable = false, precision = 12, scale = 2)
private BigDecimal settlementAmount;

@NotBlank // 공백 방어
@Column(nullable = false, length = 100, unique = true)
private String email;
```
- 금액의 경우 0을 허용하되, bean validation에는 `@PositiveOrZero` 어노테이션으로 검증한다
- `@Column(precision = 12, scale = 2)`은 엔티티에, `@Digits(integer = 10, fraction = 2)`는 dto에 적용한다.
- 문자열은 `@NotBlank` 사용 (Null + 공백 미허가)
