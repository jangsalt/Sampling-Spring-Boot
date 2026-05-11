# 06. 내부 클래스 DTO 패턴

## 개요

같은 도메인의 입출력/조회 DTO를 **외부 클래스 하나에 내부 static class로 묶는다**. 클래스 파일 수가 줄고, 사용처에서 의미가 명확해진다 (`UserDTO.Request`, `UserDTO.Response`).

## 위치

- [src/main/java/me/jangsalt/sampling/feature/user/dto/UserDTO.java](../../../src/main/java/me/jangsalt/sampling/feature/user/dto/UserDTO.java)

## 구조

```java
public class UserDTO {

    private UserDTO() {}            // ★ 인스턴스화 방지 (네임스페이스 역할)

    public static class Request  { ... }   // 신규 등록/수정 입력
    public static class Search   { ... }   // 검색 조건
    public static class Response { ... }   // 조회 응답 (마스킹 적용)
}
```

## 호출 코드에서의 가독성

```java
// 외부 사용
public UserDTO.Response createUser(@RequestBody UserDTO.Request request) { ... }
public List<UserDTO.Response> getUsers(@ModelAttribute UserDTO.Search search) { ... }
```

도메인(User)과 용도(Request/Search/Response)를 한 줄에서 한눈에 파악할 수 있다.

## 비교

| 방식 | 클래스 수 | 호출부 표기 | 단점 |
|---|---|---|---|
| 분리 파일 (`UserRequest`, `UserResponse`, `UserSearch`) | 3 | `UserRequest` | 도메인이 분명하지 않음, 도메인 늘면 파일 폭증 |
| **내부 클래스** (현 구현) | 1 (+ inner) | `UserDTO.Request` | import 한 줄로 모두 사용 가능, 응집도 ↑ |
| Records + sealed | Java 21 | `UserDTO.Request(...)` | Lombok 대비 학습 곡선, 마스킹 어노테이션과 인터액션 검증 필요 |

## DTO별 어노테이션 차이

| 내부 클래스 | Lombok | 마스킹 |
|---|---|---|
| `Request` | `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor` | 없음 (원본 그대로 받음) |
| `Search` | `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor` | 없음 (`@ModelAttribute` 바인딩 필요) |
| `Response` | `@Getter @Builder @NoArgsConstructor @AllArgsConstructor` (Setter 없음) | `@Mask(...)` 부착 |

- `Response`에 `@Setter`가 없는 이유: 응답은 한 번 생성 후 변경할 일이 없어 불변에 가깝게 둠.
- `Request`/`Search`에 `@Setter`가 필요한 이유: Jackson/Spring이 reflection으로 값 세팅. `@NoArgsConstructor` + `@Setter`가 가장 호환성 좋음.

## 주의사항

- **외부 클래스의 private 생성자**: `new UserDTO()` 호출을 막아 "UserDTO는 인스턴스화하지 않고 네임스페이스로만 쓰인다"는 의도를 명시.
- **import 한 번이면 OK**: 호출부에서 `import ...UserDTO;` 하나로 `UserDTO.Request`, `UserDTO.Response` 모두 사용 가능.
- **순환 참조 주의**: 내부 클래스가 외부 클래스를 참조해도 static이므로 인스턴스 누수는 없으나, 도메인이 커지면 분리 검토.
- **테스트 가독성**: `UserDTO.Request.builder()...` 와 같이 호출이 깊어진다. 매우 빈번한 생성이라면 `static import`도 고려.

## 확장 시 패턴

도메인이 더 복잡해지면:
```java
public class UserDTO {
    public static class Request {
        public static class Create  { ... }   // POST 전용
        public static class Update  { ... }   // PUT 전용 (id 포함)
    }
    public static class Response {
        public static class Summary { ... }   // 목록용 (간소)
        public static class Detail  { ... }   // 단건용 (전체)
    }
}
```
2단계 중첩까지가 가독성 한계. 그 이상은 별도 클래스/패키지로 분리하는 게 낫다.
