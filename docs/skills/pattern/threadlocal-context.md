# 03. ThreadLocal 컨텍스트 패턴

## 개요

요청별로 다른 동작을 해야 하는데(예: 응답 마스킹 ON/OFF), 그 상태를 메서드 시그니처로 전달하면 모든 계층이 의존하게 된다. `ThreadLocal`을 쓰면 **같은 스레드 내**에서 어디서든 상태를 읽을 수 있다.

> Spring MVC는 요청당 워커 스레드 1개를 할당하므로 `ThreadLocal` 값은 자연스럽게 **요청 스코프**가 된다.

## 위치

- [src/main/java/me/jangsalt/sampling/common/component/mask/MaskingContext.java](../../../src/main/java/me/jangsalt/sampling/common/component/mask/MaskingContext.java)

## 핵심 코드

```java
public final class MaskingContext {

    private static final ThreadLocal<Boolean> MASK_ENABLED =
            ThreadLocal.withInitial(() -> true);   // ★ 기본값 true (마스킹 적용)

    public static boolean isMaskingEnabled() { return MASK_ENABLED.get(); }
    public static void enableMasking()       { MASK_ENABLED.set(true); }
    public static void disableMasking()      { MASK_ENABLED.set(false); }
    public static void clear()               { MASK_ENABLED.remove(); }   // ★ 누수 방지
}
```

## 사용 흐름

```
[요청 시작]
   │
   ▼
MaskingInterceptor.preHandle()        ─→  MaskingContext.disable/enable
   │
   ▼
@RestController 처리
   │
   ▼
Jackson 직렬화
  └ MaskSerializer.serialize()        ─→  MaskingContext.isMaskingEnabled()  ← 여기서 조회
   │
   ▼
MaskingInterceptor.afterCompletion()  ─→  MaskingContext.clear()
   │
   ▼
[요청 종료]
```

## ThreadLocal 누수 (반드시 알아야 함)

서블릿 컨테이너는 **스레드 풀**로 워커를 재사용한다. 어떤 요청이 `MaskingContext.disableMasking()`을 호출하고 정리 없이 끝나면, 그 스레드가 다음 요청에 재할당될 때 잘못된 값이 남는다.

**대응:**
- 인터셉터 `afterCompletion()`에서 반드시 `clear()` 호출 ([HandlerInterceptor](../spring-mvc/handler-interceptor.md))
- `try-finally` 패턴이 안전하지만, 인터셉터의 `afterCompletion`은 예외가 발생해도 호출되므로 등가
- WAS 종료 시 컨테이너에 의해 메모리 해제됨 — 다만 운영 중 누수가 메모리/로직 양쪽으로 위험

## `withInitial` vs `set`

```java
// 패턴 A: withInitial - 첫 get() 시 기본값 lazy 초기화
private static final ThreadLocal<Boolean> X = ThreadLocal.withInitial(() -> true);

// 패턴 B: 매 요청마다 명시적으로 set
private static final ThreadLocal<Boolean> Y = new ThreadLocal<>();
// 사용처: Y.set(true);
```

본 프로젝트는 **A**를 채택했다. 이유: 인터셉터에서 `disableMasking()`이 호출되지 않은 요청에 대해 기본값(true)을 자동 보장.

## 대안 패턴

| 패턴 | 장점 | 단점 |
|---|---|---|
| **ThreadLocal** (현 구현) | 단순, 매개변수 전파 불필요 | 누수 위험, 비동기/리액티브 호환성 ↓ |
| `RequestContextHolder` (Spring) | Spring이 정리 책임 | Spring MVC 결합 |
| `@RequestScope` 빈 | 의존성 주입 | 직렬화기 등 빈이 아닌 객체에서 접근 어려움 |
| 메서드 파라미터 전달 | 명시적 | 모든 계층 시그니처 오염 |
| Reactive `Context` (WebFlux) | 리액티브 안전 | MVC 환경에 불필요 |

## 주의사항

- **비동기 처리 시 주의**: `@Async`, `CompletableFuture.supplyAsync()` 등으로 다른 스레드로 작업이 넘어가면 ThreadLocal 값은 **전파되지 않는다**. `TaskDecorator`로 컨텍스트 복사가 필요.
- **WebFlux 사용 시 부적합**: Netty/리액티브 환경에서는 한 요청이 여러 스레드를 거치므로 `Reactor Context`를 사용해야 한다.
