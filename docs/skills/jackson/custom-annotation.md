# 01. 커스텀 어노테이션 + Jackson 직렬화

## 개요

필드에 부착하는 마커 어노테이션 `@Mask`를 만들고, Jackson 메타-어노테이션 `@JacksonAnnotationsInside`를 통해 어노테이션 자체가 Jackson에게 시리얼라이저 위임을 지시하도록 한다. 사용 측은 단순히 `@Mask(MaskType.NAME)`만 붙이면 직렬화 시 자동 마스킹이 적용된다.

## 위치

- [src/main/java/me/jangsalt/sampling/common/component/mask/Mask.java](../../../src/main/java/me/jangsalt/sampling/common/component/mask/Mask.java)
- [src/main/java/me/jangsalt/sampling/common/component/mask/MaskType.java](../../../src/main/java/me/jangsalt/sampling/common/component/mask/MaskType.java)

## 핵심 코드

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside                          // ★ Jackson이 nested annotation을 인식하게 함
@JsonSerialize(using = MaskSerializer.class)       // ★ 이 어노테이션이 부착된 필드에 시리얼라이저 적용
public @interface Mask {
    MaskType value() default MaskType.DEFAULT;
}
```

## 동작 원리

1. Jackson은 DTO 필드의 어노테이션을 스캔할 때 `@JacksonAnnotationsInside`가 붙은 메타 어노테이션도 함께 탐색한다.
2. `@Mask`가 부착된 필드를 만나면 내부의 `@JsonSerialize`를 인식하여 `MaskSerializer`로 직렬화를 위임한다.
3. `MaskSerializer`는 `ContextualSerializer`를 통해 어떤 `MaskType`인지 어노테이션 값을 읽어 처리한다 (→ [커스텀 시리얼라이저](custom-serializer.md)).

## 사용

```java
public class UserDTO {
    public static class Response {
        @Mask(MaskType.NAME)
        private String name;        // 홍길동 → 홍*동

        @Mask(MaskType.EMAIL)
        private String email;       // hong@example.com → h***@example.com

        @Mask                       // 기본값: DEFAULT (전체 마스킹)
        private String password;    // secret → ******
    }
}
```

## 왜 메타 어노테이션 방식인가?

| 방식 | 장점 | 단점 |
|---|---|---|
| **`@Mask` 메타** (현 구현) | 의미 있는 이름 (`@Mask`), 한 줄 적용 | Jackson 동작 학습 필요 |
| 필드마다 `@JsonSerialize(using=...)` | Jackson 기본 API만 사용 | 보일러플레이트 증가, 의도 불명확 |
| AOP로 응답 객체 후처리 | Jackson 외부 | 성능 비용, 컬렉션/중첩 처리 복잡 |
| Servlet Filter 응답 변환 | 자유도 높음 | JSON 파싱/재직렬화 비용, 위험 |

## 주의사항

- `@JacksonAnnotationsInside`는 **반드시** 필요하다. 없으면 Jackson은 `@Mask` 내부의 `@JsonSerialize`를 발견하지 못해 시리얼라이저가 적용되지 않는다.
- `@Target(ElementType.FIELD)`로 제한했지만, 메서드(getter)에도 적용하려면 `METHOD`를 추가해야 한다.
- `MaskType.DEFAULT`를 `value()`의 기본값으로 두어 `@Mask` 단독 사용을 허용한다.

## 확장 아이디어

- **타입별 정책 외부화**: `MaskType`을 enum 대신 `interface MaskingStrategy`로 만들고 빈으로 등록하면 런타임 교체 가능.
- **부분 노출 비율 파라미터화**: `@Mask(value = NAME, expose = 2)` 식으로 노출 글자 수를 어노테이션 속성으로 추가.
- **메타 어노테이션 조합**: `@PiiName`, `@PiiEmail` 같은 도메인 친화 이름을 만들고 내부에서 `@Mask(NAME)` 위임.
