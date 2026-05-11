# 전역 예외 처리 (비즈니스 ↔ 프레임워크 분리)

## 개요

`@RestControllerAdvice`를 **두 개 분리**하여 비즈니스(내부 로직) 예외와 프레임워크/JDK 예외를 다른 클래스에서 처리한다. 응답은 [`CommonResponse`](common-response.md) envelope를 그대로 사용한다.

- **`BusinessExceptionHandler`**: 우리가 던지는 `BusinessException`만 처리. 우선순위 최상.
- **`GlobalExceptionHandler`**: Spring/JDK 예외와 알 수 없는 예외 처리. 우선순위 최하.

## 위치

- [common/exception/BusinessException.java](../../../src/main/java/me/jangsalt/sampling/common/exception/BusinessException.java)
- [common/exception/ErrorCode.java](../../../src/main/java/me/jangsalt/sampling/common/exception/ErrorCode.java)
- [common/exception/handler/BusinessExceptionHandler.java](../../../src/main/java/me/jangsalt/sampling/common/exception/handler/BusinessExceptionHandler.java)
- [common/exception/handler/GlobalExceptionHandler.java](../../../src/main/java/me/jangsalt/sampling/common/exception/handler/GlobalExceptionHandler.java)
- [common/exception/handler/ErrorMessageFormatter.java](../../../src/main/java/me/jangsalt/sampling/common/exception/handler/ErrorMessageFormatter.java)

## 응답 형식

응답 envelope는 [`CommonResponse`](common-response.md)와 동일하다:

```json
{
  "code": 404,
  "message": "[U0001] 사용자를 찾을 수 없습니다. (id=999)",
  "data": null,
  "time": "2026-05-11T13:29:05.537236"
}
```

| 필드 | 의미 |
|---|---|
| `code` | HTTP 상태 코드 (int) |
| `message` | `[에러코드] 표준 메시지 (디테일)` — `ErrorMessageFormatter`가 생성 |
| `data` | 항상 `null` (에러 응답이므로) |
| `time` | 응답 생성 시각 |

에러 코드(`U0001` 등)는 `message` 안에 대괄호로 포함되어 노출된다.

## ErrorCode 설계

```java
public enum ErrorCode {
    INVALID_INPUT      ("E0001", "잘못된 입력입니다.", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND     ("U0001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INTERNAL_ERROR     ("E0500", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ...
}
```

**코드 체계 규칙:**
- `E0xxx` — 공통 (Common)
- `{도메인 약자}0xxx` — 도메인별 (예: `U` for User)
- 4자리 숫자 — 첫 자리는 HTTP 상태 분류 (`E04xx` ≈ 4xx), 나머지는 순번

## BusinessException

```java
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode ec) {
        super(ec.getMessage());
        this.errorCode = ec;
    }

    public BusinessException(ErrorCode ec, String detail) {
        super(detail);
        this.errorCode = ec;
    }
}
```

**왜 `RuntimeException`인가?**

- 체크 예외(checked)는 서비스 ↔ 컨트롤러 ↔ 어딘가에서 `throws` 선언이 강제되어 코드 오염.
- 비즈니스 예외는 본질적으로 "예측 가능한 흐름의 일부"가 아닌 **예외 케이스**라 unchecked가 적합.
- Spring `@Transactional`은 unchecked 예외에서만 롤백이 기본 동작.

## 사용 (Service)

```java
public UserDTO.Response findById(Long id) {
    User user = users.get(id);
    if (user == null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND, "id=" + id);
    }
    return user.toResponse();
}
```

`Service`는 HTTP를 모른다. 어떤 상태 코드/응답 형식을 줄지는 **`ErrorCode`와 핸들러**가 결정. 서비스는 "도메인 사실"만 던진다.

## BusinessExceptionHandler

```java
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class BusinessExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handle(BusinessException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        String detail = !ex.getMessage().equals(errorCode.getMessage()) ? ex.getMessage() : null;
        String message = ErrorMessageFormatter.format(errorCode, detail);

        log.warn("BusinessException: {} | path={}", message, request.getRequestURI());

        return ResponseEntity.status(errorCode.getStatus())
                .body(CommonResponse.error(errorCode.getStatus().value(), message));
    }
}
```

## GlobalExceptionHandler

처리 대상:

| 예외 | 응답 코드 | 로그 레벨 |
|---|---|---|
| `HttpMessageNotReadableException` (JSON parse 실패) | `E0001` / 400 | `INFO` |
| `MethodArgumentTypeMismatchException` (path/param 타입 불일치) | `E0001` / 400 | `INFO` |
| `HttpRequestMethodNotSupportedException` (잘못된 HTTP method) | `E0405` / 405 | `INFO` |
| `Exception.class` (catch-all) | `E0500` / 500 | `ERROR` (stack trace 포함) |

**catch-all은 detail을 null로 전달한다** — 내부 메시지가 응답에 노출되면 정보 유출 위험.

## 두 핸들러의 우선순위

```java
@Order(Ordered.HIGHEST_PRECEDENCE)   // BusinessExceptionHandler
@Order(Ordered.LOWEST_PRECEDENCE)    // GlobalExceptionHandler
```

Spring은 여러 `@RestControllerAdvice` 중 `@Order`가 낮은(우선순위 높은) 것부터 먼저 핸들러를 탐색한다. `BusinessException`은 `Exception`의 하위 클래스이므로, Global의 `Exception.class` 핸들러가 먼저 잡지 못하도록 **Business를 위에 둬야 한다**.

> 둘 다 `@Order`를 명시하지 않으면 클래스 로딩 순서에 따라 동작이 달라질 수 있어 운영 환경에서 위험.

## 실측 응답

| 케이스 | 요청 | HTTP | code |
|---|---|---|---|
| 비즈니스 (없는 ID) | `GET /api/users/999` | 404 | `U0001` |
| 타입 불일치 | `GET /api/users/abc` | 400 | `E0001` |
| 잘못된 메서드 | `DELETE /api/users/1` | 405 | `E0405` |
| JSON 파싱 실패 | `POST /api/users` with `{bad json}` | 400 | `E0001` |

## 확장

### 새 도메인 예외 추가
```java
// 1. ErrorCode에 enum 값 추가
ORDER_ALREADY_PAID("O0010", "이미 결제된 주문입니다.", HttpStatus.CONFLICT);

// 2. 서비스에서 throw — 별도 핸들러 추가 불필요
throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
```

새로운 예외 타입 추가 시 `BusinessExceptionHandler`/`GlobalExceptionHandler` 수정이 거의 없다.

### Validation (`@Valid`) 처리
필요 시 `GlobalExceptionHandler`에 다음을 추가:
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, ...) {
    // BindingResult → 필드별 에러 메시지 직렬화
}
```

### 권한 (Spring Security)
`AccessDeniedException`, `AuthenticationException`은 보통 Security 필터 단계에서 처리되어 advice에 도달하지 않는다. `AccessDeniedHandler`/`AuthenticationEntryPoint`에서 같은 `ErrorResponse` 형식으로 응답하도록 별도 구현 필요.

## 주의사항

- **로그 레벨 분리**: 클라이언트 측 원인(4xx)은 `INFO`/`WARN`, 서버 측 원인(5xx)은 `ERROR`로 분리해 운영 시 노이즈 최소화.
- **detail 노출 정책**: 비즈니스 예외는 detail을 응답에 포함 (사용자에게 컨텍스트 제공). 서버 내부 오류는 detail을 비움 (정보 유출 방지).
- **`@RestControllerAdvice` 범위**: 기본은 전체 컨트롤러 대상. 특정 패키지/어노테이션만 적용하려면 `@RestControllerAdvice(basePackages = "...")` 또는 `(annotations = ...)` 옵션 사용.
- **`ResponseStatusException` 미사용**: Spring이 제공하는 `ResponseStatusException`도 같은 목적이지만, 상태와 메시지가 코드 곳곳에 흩어져 일관성이 떨어진다. `BusinessException` + `ErrorCode`로 중앙 집중 관리.
