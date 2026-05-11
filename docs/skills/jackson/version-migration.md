# 08. Spring Boot 3.5 ↔ 4.0 Jackson API 변경

## 개요

Spring Boot 4.0은 Jackson 메이저 업그레이드(2.x → 3.x)를 동반한다. **패키지 경로**와 **클래스 명/시그니처**가 모두 바뀌어 커스텀 시리얼라이저 코드는 거의 전 라인이 수정 대상이 된다. 본 문서는 두 버전의 차이와 이행 방법을 정리한다.

## 본 프로젝트의 현재 상태

- Spring Boot **3.5.14** + Jackson **2.x** 기준으로 작성됨
- Spring Boot 4.0으로 이행 시 아래 표대로 일괄 치환 필요

## 차이 요약

| 항목 | Spring Boot 3.5 (Jackson 2.x) | Spring Boot 4.0 (Jackson 3.x) |
|---|---|---|
| 패키지 (core/databind) | `com.fasterxml.jackson.core` / `com.fasterxml.jackson.databind` | `tools.jackson.core` / `tools.jackson.databind` |
| 패키지 (annotations) | `com.fasterxml.jackson.annotation` | 동일 (변경 없음) |
| 시리얼라이저 베이스 | `JsonSerializer<T>` (abstract class) | `ValueSerializer<T>` (abstract class) |
| 컨텍스트 인터페이스 | `ContextualSerializer` (별도 구현) | `ValueSerializer.createContextual()` (베이스 통합) |
| `serialize()` 3번째 인자 | `SerializerProvider` | `SerializationContext` |
| 예외 처리 | `throws IOException` (checked) | unchecked (`JacksonException extends RuntimeException`) |
| `@JsonSerialize` 위치 | `com.fasterxml.jackson.databind.annotation.JsonSerialize` | `tools.jackson.databind.annotation.JsonSerialize` |

## 코드 비교

### `Mask.java`

**3.5 (현재):**
```java
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;     // ★

@JacksonAnnotationsInside
@JsonSerialize(using = MaskSerializer.class)
public @interface Mask { ... }
```

**4.0:**
```java
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import tools.jackson.databind.annotation.JsonSerialize;             // ★

@JacksonAnnotationsInside
@JsonSerialize(using = MaskSerializer.class)
public @interface Mask { ... }
```

### `MaskSerializer.java`

**3.5 (현재):**
```java
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import java.io.IOException;

public class MaskSerializer
        extends JsonSerializer<String>
        implements ContextualSerializer {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException { ... }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) { ... }
}
```

**4.0:**
```java
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class MaskSerializer extends ValueSerializer<String> {       // ★ implements 제거

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {   // ★ throws 제거
        ...
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) { ... }
}
```

## 일괄 치환 가이드

루트 디렉토리에서 (4.0 → 3.5로 다운그레이드 예시):
```bash
find src -name "*.java" -exec sed -i '' \
  -e 's|tools\.jackson\.core|com.fasterxml.jackson.core|g' \
  -e 's|tools\.jackson\.databind|com.fasterxml.jackson.databind|g' {} \;

# 그 후 수동 수정:
# - ValueSerializer → JsonSerializer
# - SerializationContext → SerializerProvider
# - serialize() 에 throws IOException 추가
# - implements ContextualSerializer 추가
```

3.5 → 4.0 업그레이드는 반대 방향으로 진행.

## 변경 불필요한 파일

다음 파일들은 Jackson 직접 API를 쓰지 않아 두 버전에서 그대로 사용 가능:
- `MaskType`, `MaskingUtil`, `MaskingContext` — 순수 Java
- `MaskingInterceptor`, `WebConfig` — Spring MVC API (jakarta servlet)
- `UserDTO`, `UserService`, `UserController`, `User` — Spring Boot API

> Jackson 추상화에 잘 분리해두면 Jackson 메이저 업그레이드 영향이 시리얼라이저 1~2개 파일로 한정된다.

## pom.xml 변경

| | Spring Boot 3.5 | Spring Boot 4.0 |
|---|---|---|
| parent version | `3.5.14` | `4.0.6` |
| 최소 Java | 17 | 17 (21+ 권장) |
| Jackson 의존성 | 자동 (`com.fasterxml.jackson.*`) | 자동 (`tools.jackson.*`) |

## 어떤 버전을 선택해야 하나?

| 상황 | 권장 |
|---|---|
| 기존 3.x 운영 프로젝트 | 3.5.14+로 유지, OSS 지원 종료(2026-06-30) 전에 4.x 전환 계획 |
| 신규 프로젝트 | **4.0.6+** 권장 (장기 호환성, Jackson 3 신기능) |
| 의존 라이브러리가 Jackson 2 종속 | 3.5.x 유지 또는 4.x + Jackson 2 호환 라이브러리 등장 대기 |

## 참고 링크

- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Jackson 3.0 Migration Guide](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0)
- [CVE-2026-40972 (Spring Boot DevTools)](https://spring.io/security/cve-2026-40972)
