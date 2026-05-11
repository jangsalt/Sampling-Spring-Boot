# 02. Jackson 커스텀 시리얼라이저

## 개요

`JsonSerializer`를 상속하여 특정 타입(`String`)의 JSON 직렬화 방식을 커스터마이즈한다. `ContextualSerializer`를 함께 구현하면 **어노테이션의 속성 값을 읽어 시리얼라이저 인스턴스를 필드별로 분기**할 수 있다.

## 위치

- [src/main/java/me/jangsalt/sampling/common/component/mask/MaskSerializer.java](../../../src/main/java/me/jangsalt/sampling/common/component/mask/MaskSerializer.java)
- [src/main/java/me/jangsalt/sampling/common/component/mask/MaskingUtil.java](../../../src/main/java/me/jangsalt/sampling/common/component/mask/MaskingUtil.java)

## 핵심 코드

```java
public class MaskSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final MaskType maskType;

    public MaskSerializer() { this(MaskType.DEFAULT); }        // ★ Jackson이 기본 인스턴스 생성용

    private MaskSerializer(MaskType maskType) {                // ★ Contextual 생성 시에만 사용
        this.maskType = maskType;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (!MaskingContext.isMaskingEnabled()) {              // ★ 요청별 토글 (ThreadLocal Context 참고)
            gen.writeString(value);
            return;
        }
        gen.writeString(MaskingUtil.mask(value, maskType));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        if (property != null) {
            Mask annotation = property.getAnnotation(Mask.class);  // ★ 필드의 @Mask(...) 속성 추출
            if (annotation != null) {
                return new MaskSerializer(annotation.value());     // 타입별로 새 인스턴스 반환
            }
        }
        return this;
    }
}
```

## ContextualSerializer가 필요한 이유

Jackson은 시리얼라이저를 **타입 단위로 캐싱**한다. 즉 `MaskSerializer` 인스턴스 하나가 모든 `String` 필드에 공유된다. 따라서 `@Mask(NAME)`인 필드와 `@Mask(EMAIL)`인 필드를 구분하려면, **호출 시점에 어노테이션을 보고 새 인스턴스를 반환**해야 한다. 이를 위한 훅이 `createContextual()`이다.

- 호출 시점: 각 필드의 첫 직렬화 시 1회
- 반환된 인스턴스는 해당 필드용으로 캐시됨 → 이후 호출은 모두 그 인스턴스를 사용

## `MaskingUtil` (도메인 로직)

마스킹 규칙은 별도 유틸 클래스에 분리하여 시리얼라이저 외부에서도 호출 가능하게 한다.

```java
MaskingUtil.mask("홍길동", MaskType.NAME);        // "홍*동"
MaskingUtil.mask("hong@example.com", EMAIL);     // "h***@example.com"
```

각 타입별 알고리즘은 [MaskingUtil.java](../../../src/main/java/me/jangsalt/sampling/common/component/mask/MaskingUtil.java) 주석 참고.

## 주의사항

- **기본 생성자 필수**: Jackson은 시리얼라이저를 `@JsonSerialize(using = X.class)`에서 발견할 때 reflection으로 인스턴스화하므로 public no-arg 생성자가 필요하다.
- **null 처리**: `serialize()`는 Jackson이 non-null 값에서만 호출하므로 메서드 내부의 null 체크는 불필요. 다만 `MaskingUtil.mask()` 자체는 안전성을 위해 null 체크를 둠.
- **불변 필드**: `maskType`은 `final`로 둬 시리얼라이저 인스턴스가 스레드 간 공유돼도 안전.
- **체크 예외 (Spring Boot 3.5 = Jackson 2)**: `serialize()`는 `throws IOException`을 명시해야 한다. Jackson 3 (Spring Boot 4)에서는 `JacksonException`이 unchecked로 바뀌어 `throws` 불필요 → [버전 마이그레이션](version-migration.md).
