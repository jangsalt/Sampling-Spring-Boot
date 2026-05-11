# 04. HandlerInterceptor + WebMvcConfigurer

## 개요

`HandlerInterceptor`는 컨트롤러 진입 전후에 횡단 관심사(인증/로깅/요청 컨텍스트 세팅 등)를 처리하는 Spring MVC 훅이다. 본 프로젝트에서는 `?mask=false` 쿼리를 읽어 [MaskingContext](../pattern/threadlocal-context.md)를 토글하는 데 사용한다.

## 위치

- [src/main/java/me/jangsalt/sampling/common/component/mask/MaskingInterceptor.java](../../../src/main/java/me/jangsalt/sampling/common/component/mask/MaskingInterceptor.java)
- [src/main/java/me/jangsalt/sampling/common/config/WebConfig.java](../../../src/main/java/me/jangsalt/sampling/common/config/WebConfig.java)

## 인터셉터 구현

```java
@Component
public class MaskingInterceptor implements HandlerInterceptor {

    private static final String MASK_PARAM = "mask";

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        String value = req.getParameter(MASK_PARAM);
        if ("false".equalsIgnoreCase(value)) {
            MaskingContext.disableMasking();
        } else {
            MaskingContext.enableMasking();           // ★ 기본은 마스킹 ON
        }
        return true;                                  // ★ false 반환 시 컨트롤러 호출 자체가 중단됨
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        MaskingContext.clear();                       // ★ ThreadLocal 정리 (필수)
    }
}
```

## WebMvcConfigurer로 등록

```java
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final MaskingInterceptor maskingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(maskingInterceptor);
        // .addPathPatterns("/api/**")              // 특정 경로만
        // .excludePathPatterns("/api/health");      // 제외 경로
    }
}
```

## 인터셉터의 3개 훅

| 훅 | 호출 시점 | 반환값 의미 |
|---|---|---|
| `preHandle` | 컨트롤러 호출 **직전** | `false` 반환 시 요청 중단 (응답 직접 작성 필요) |
| `postHandle` | 컨트롤러 호출 **직후**, 뷰 렌더링 전 | `ModelAndView` 수정 가능 (REST 응답엔 무용) |
| `afterCompletion` | 응답 완료 후 (예외 여부 무관) | 정리 작업용 — `clear()`, 로그 finalize 등 |

## 실행 순서 (체이닝)

여러 인터셉터가 있을 때:

```
preHandle(A) → preHandle(B) → 컨트롤러 → postHandle(B) → postHandle(A)
                                       └→ afterCompletion(B) → afterCompletion(A)
```

`afterCompletion`은 **역순**으로 호출된다. 컨텍스트 정리 순서가 의미 있을 때 주의.

## 비교: 인터셉터 vs Filter vs AOP

| 기능 | Filter | HandlerInterceptor | AOP (@Around) |
|---|---|---|---|
| 동작 위치 | Servlet 컨테이너 단 | Spring MVC 디스패처 안 | 빈 메서드 호출 |
| 핸들러(컨트롤러) 메서드 정보 | ✗ | ✓ | ✓ |
| Spring 빈 DI | 조금 까다로움 | ✓ 자연스러움 | ✓ |
| 본 프로젝트 적합도 | 과한 추상화 | **✓ 최적** | 컨트롤러 외 비즈니스 메서드까지 영향 |

## 주의사항

- **`@Component` 등록**: 인터셉터 자체를 빈으로 등록해야 `WebConfig`에서 주입받아 사용 가능.
- **`preHandle` 반환값**: `false` 시 응답을 직접 작성하지 않으면 클라이언트는 빈 응답을 받는다. 인증 실패 시는 `response.sendError(401)` 후 `return false`.
- **`afterCompletion`은 예외 시에도 호출됨** — `ex` 파라미터로 발생 예외 접근 가능.
- 본 프로젝트는 모든 경로에 인터셉터를 적용했지만, 마스킹이 필요 없는 경로(헬스체크, 정적 리소스)는 `excludePathPatterns`로 빼는 게 권장.
