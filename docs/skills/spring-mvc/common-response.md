# 공통 응답 Envelope (`CommonResponse`)

## 개요

모든 REST 응답을 동일 구조로 통일한다. 성공/실패 응답이 일관되어 클라이언트 파싱이 단순해지고, 에러 정보(코드/메시지)가 항상 같은 위치에 있어 처리 로직을 표준화할 수 있다.

## 응답 구조

| 필드 | 의미 | 예시 |
|---|---|---|
| `code` | HTTP 상태 코드 (int) | `200`, `404`, `500` |
| `message` | 성공 시 `"OK"`, 실패 시 `[에러코드] 표준메시지 (디테일)` | `"[U0001] 사용자를 찾을 수 없습니다. (id=999)"` |
| `data` | 응답 페이로드 (실패 시 `null`) | `{ "id": 1, ... }` |
| `time` | 응답 생성 시각 (ISO-8601) | `"2026-05-11T13:34:45.510174"` |

### 성공 응답 예시
```json
{
  "code": 200,
  "message": "OK",
  "data": { "id": 1, "name": "홍*동", "email": "h***@example.com", ... },
  "time": "2026-05-11T13:34:45.510174"
}
```

### 에러 응답 예시
```json
{
  "code": 404,
  "message": "[U0001] 사용자를 찾을 수 없습니다. (id=999)",
  "data": null,
  "time": "2026-05-11T13:34:45.946590"
}
```

## 위치

- [common/response/CommonResponse.java](../../../src/main/java/me/jangsalt/sampling/common/response/CommonResponse.java)
- [common/response/CommonResponseAdvice.java](../../../src/main/java/me/jangsalt/sampling/common/response/CommonResponseAdvice.java)
- [common/exception/handler/ErrorMessageFormatter.java](../../../src/main/java/me/jangsalt/sampling/common/exception/handler/ErrorMessageFormatter.java)

## 동작 원리

```
[Controller가 DTO 반환]
        ↓
ResponseBodyAdvice.beforeBodyWrite()    ← CommonResponseAdvice
        ↓
   body가 CommonResponse?  ──Yes──→ 그대로 통과 (이중 래핑 방지)
        ↓ No
   body가 String?         ──Yes──→ 그대로 통과 (StringHttpMessageConverter 호환)
        ↓ No
   CommonResponse.success(httpStatus, body) 로 감싸기
        ↓
   Jackson 직렬화 (Mask 시리얼라이저는 data 안의 필드에 그대로 적용됨)
        ↓
   클라이언트 응답
```

예외 경로:
```
[Service에서 BusinessException 발생]
        ↓
BusinessExceptionHandler.handle()
        ↓
   ErrorMessageFormatter.format(errorCode, detail)  →  "[U0001] 메시지 (디테일)"
        ↓
   ResponseEntity.status(404).body(CommonResponse.error(404, message))
        ↓
ResponseBodyAdvice 가 supports()에서 false 반환 (이미 CommonResponse) → 통과
        ↓
   클라이언트 응답
```

## CommonResponse 핵심 코드

```java
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonResponse<T> {

    private final int code;
    private final String message;
    private final T data;
    private final LocalDateTime time;

    public static <T> CommonResponse<T> success(int code, T data) {
        return new CommonResponse<>(code, "OK", data, LocalDateTime.now());
    }

    public static CommonResponse<Void> error(int code, String message) {
        return new CommonResponse<>(code, message, null, LocalDateTime.now());
    }
}
```

- 생성자 private + 정적 팩토리 → 의도가 다른 두 가지 생성 경로(`success` vs `error`)를 명시
- `code`는 `int` (HTTP status). `HttpStatus.value()` 호출 결과 그대로
- `time`은 `LocalDateTime` — Spring Boot 기본 설정으로 ISO-8601 문자열 직렬화

## CommonResponseAdvice 핵심 코드

```java
@RestControllerAdvice
public class CommonResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !CommonResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, ...) {
        if (body instanceof CommonResponse<?>) return body;
        if (body instanceof String) return body;
        return CommonResponse.success(currentStatus(response), body);
    }
}
```

### `supports()`의 역할

`returnType`이 `CommonResponse<?>`인 컨트롤러(예: 직접 envelope를 반환하는 경우)는 `supports()`에서 false를 반환해 advice 호출 자체를 막는다.

### `String` 우회

Spring은 컨트롤러 반환 타입이 `String`이면 `StringHttpMessageConverter`를 사용한다. 이 컨버터는 `CommonResponse` 객체를 받아도 JSON으로 직렬화하지 못한다. 따라서 String은 래핑하지 않고 통과시킨다.

> 항상 envelope를 보장하려면 `produces = MediaType.APPLICATION_JSON_VALUE`를 명시하거나 컨트롤러가 String 대신 `Map<String, String>`을 반환하도록 한다.

## 메시지 포맷 (`ErrorMessageFormatter`)

```java
[USER_NOT_FOUND, "id=999"]  →  "[U0001] 사용자를 찾을 수 없습니다. (id=999)"
[USER_NOT_FOUND, null]      →  "[U0001] 사용자를 찾을 수 없습니다."
```

`code` 필드와 `message` 필드 모두에서 에러 코드를 확인할 수 있어 클라이언트는 둘 중 편한 방식을 선택한다:
- 단순 표시 → `message` 그대로 출력
- 코드 기반 분기 → `message`에서 `[XXXX]` 추출 또는 별도 코드 필드 추가 검토

## 마스킹과의 인터랙션

`CommonResponse.data` 안의 DTO에 `@Mask` 어노테이션이 있어도 정상 동작한다:
1. Jackson은 envelope를 직렬화하다가 `data` 필드의 DTO를 만남
2. DTO의 필드에 부착된 `@Mask` 메타 어노테이션이 발견됨
3. `MaskSerializer`가 활성화되어 해당 필드값 마스킹

즉 envelope는 직렬화 트리의 최상위에만 영향을 주고, 내부 객체의 어노테이션 처리에는 간섭하지 않는다.

## 주의사항

- **`ResponseEntity` 반환 시 status 보존**: `ResponseEntity.status(201).body(...)`로 반환하면 advice가 `response.getStatus()`로 201을 읽어 `code` 필드에 반영한다.
- **이중 래핑 방지**: `supports()`와 `beforeBodyWrite()`의 `instanceof` 체크가 둘 다 동작한다 (반환 타입이 `Object`인 핸들러에선 supports로 거를 수 없으므로 instanceof 필수).
- **HEAD 요청**: HEAD는 body가 없어 advice가 호출되지 않는다. 별도 처리 불필요.
- **Spring Security 응답**: AuthenticationEntryPoint/AccessDeniedHandler가 직접 응답을 작성하는 경우 advice를 거치지 않는다. 같은 envelope 형식으로 응답하려면 핸들러 안에서 `CommonResponse`를 직렬화해 써야 한다.
- **`time` 정밀도**: 마이크로초까지 출력되는데, 클라이언트가 파싱 시 ISO-8601 fractional second 지원을 확인할 것. 고정 포맷 원하면 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` 추가.

## 비교: 왜 ResponseBodyAdvice를 쓰는가?

| 방식 | 장점 | 단점 |
|---|---|---|
| **`ResponseBodyAdvice`** (현 구현) | 컨트롤러 코드에 한 줄도 안 닿음, 자동 적용 | Spring MVC 지식 필요, 우회 시 advice 비활성 |
| 컨트롤러가 직접 `CommonResponse` 반환 | 명시적, 디버깅 쉬움 | 모든 컨트롤러에 보일러플레이트 |
| AOP `@Around` | 메서드 단위 제어 | proxy 한계, 성능 비용 |
| Servlet Filter | 프레임워크 무관 | JSON 파싱/재직렬화 비용, 위험 |
