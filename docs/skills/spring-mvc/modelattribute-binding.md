# 07. `@ModelAttribute` 쿼리 자동 바인딩

## 개요

여러 검색 조건을 받을 때 `@RequestParam` N개로 시그니처를 늘리지 않고, **DTO 객체 하나로 받는다**. Spring MVC가 쿼리 파라미터 이름을 객체 필드명에 매칭하여 자동 바인딩한다.

## 위치

- [UserController.java](../../../src/main/java/me/jangsalt/sampling/feature/user/controller/UserController.java)
- [UserDTO.java#Search](../../../src/main/java/me/jangsalt/sampling/feature/user/dto/UserDTO.java)

## 핵심 코드

```java
// DTO
public static class Search {
    private String name;
    private String email;
}

// Controller
@GetMapping
public List<UserDTO.Response> getUsers(@ModelAttribute UserDTO.Search search) {
    return userService.search(search);
}
```

```bash
GET /api/users?name=홍&email=hong
# → Search.name = "홍", Search.email = "hong"
```

## `@RequestParam` vs `@ModelAttribute` vs `@RequestBody`

| 어노테이션 | 데이터 출처 | 받는 형태 | 적합한 상황 |
|---|---|---|---|
| `@RequestParam` | 쿼리스트링 / `x-www-form-urlencoded` | 개별 변수 | 파라미터 1~2개 |
| **`@ModelAttribute`** | 쿼리스트링 / form 데이터 | 객체 | 검색 조건 같은 다수 필드 (현 구현) |
| `@RequestBody` | HTTP body (JSON 등) | 객체 | POST/PUT의 본문 |

## 바인딩 규칙

1. 쿼리 파라미터 이름 = DTO 필드명 (대소문자 일치, 다르면 `@JsonProperty` / `@RequestParam(name=...)`는 작동 안 함 — `@ModelAttribute` 바인딩은 reflection 기반)
2. Setter 또는 `@AllArgsConstructor` 둘 중 하나 필요. **본 프로젝트는 `@NoArgsConstructor` + `@Setter`** 조합 사용.
3. 값이 없는 파라미터는 그대로 `null`로 둠 (primitive 타입은 0/false). nullable 보장이 필요하면 wrapper 타입 사용.
4. 타입 변환은 Spring `ConversionService`가 처리 — `String` → `Long`, `Integer`, enum, date 등 자동.

## `@ModelAttribute` 생략 가능?

```java
public List<UserDTO.Response> getUsers(UserDTO.Search search) { ... }   // OK, 명시 안 해도 동일
```

Spring MVC는 핸들러 메서드의 객체 타입 파라미터에 대해 `@ModelAttribute`를 **암묵적**으로 적용한다. 하지만 의도를 분명히 드러내려면 **명시 권장**.

## 검색 서비스 매칭

```java
public List<UserDTO.Response> search(UserDTO.Search condition) {
    String name = normalize(condition.getName());
    String email = normalize(condition.getEmail());
    if (name == null && email == null) return findAll();
    return users.values().stream()
            .filter(u -> name == null || u.getName().toLowerCase().contains(name))
            .filter(u -> email == null || u.getEmail().toLowerCase().contains(email))
            ...
}
```

각 필드 nullable → null이면 그 조건은 무시(skip). 모든 조건이 null이면 전체 반환. 한 필드라도 값이 있으면 AND 필터.

## 새 검색 필드 추가 방법

1. `UserDTO.Search`에 필드 추가
2. `UserService.search()`에 `.filter(...)` 한 줄 추가
3. Controller / 클라이언트 변경 불필요 (Spring이 자동 바인딩)

## 주의사항

- **POST + body 검색**: 검색 조건이 매우 많거나 민감하다면 `?...` 쿼리 대신 POST + `@RequestBody`로 받기도 한다. URL 길이 한계(브라우저별 2~8KB)와 로그 노출을 피할 수 있다.
- **검증**: 검색 DTO에도 `@Valid` 적용 가능 (`@Size`, `@Pattern` 등). 본 프로젝트는 검증 없이 normalize로 처리.
- **컬렉션 바인딩**: `?ids=1&ids=2&ids=3` → `List<Long> ids` 자동 바인딩 가능. `?ids=1,2,3`은 기본은 안 되며 별도 컨버터 필요.
