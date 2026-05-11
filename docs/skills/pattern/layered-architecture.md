# 05. 계층형 아키텍처 (feature 단위 패키징)

## 개요

레이어 책임을 명확히 분리하고, **기능(feature) 단위로 패키지를 묶는다**. 전통적 "layer-first" (`controller/*`, `service/*`, ...) 대신 "feature-first" (`feature/user/controller/...`)를 채택했다.

## 디렉토리 구조

```
me.jangsalt.sampling/
├── common/                # 공통 컴포넌트 (도메인 무관)
│   ├── component/mask/    # 마스킹 컴포넌트
│   └── config/            # 전역 설정 (WebConfig 등)
└── feature/               # 기능별 모듈
    └── user/
        ├── controller/    # HTTP 진입
        ├── service/       # 비즈니스 로직
        ├── entity/        # 도메인 엔티티
        └── dto/           # 데이터 전달 객체
```

## 계층별 책임

| 계층 | 책임 | 파일 |
|---|---|---|
| Controller | HTTP 요청 매핑, 입출력 DTO 사용, Service 위임만 | [UserController.java](../../../src/main/java/me/jangsalt/sampling/feature/user/controller/UserController.java) |
| Service | 비즈니스 규칙, 트랜잭션 경계, Entity ↔ DTO 변환 | [UserService.java](../../../src/main/java/me/jangsalt/sampling/feature/user/service/UserService.java) |
| Entity | 도메인 상태, 표현 계층 관심사 미포함 | [User.java](../../../src/main/java/me/jangsalt/sampling/feature/user/entity/User.java) |
| DTO | 계층 간 데이터 전달, 직렬화 어노테이션 부착 | [UserDTO.java](../../../src/main/java/me/jangsalt/sampling/feature/user/dto/UserDTO.java) |

## 의존 방향

```
Controller → Service → Entity
       ↘─ DTO ─↗
              (Entity가 DTO를 import: toResponse() 변환 메서드용 — 양방향 의존 주의)
```

> 본 프로젝트는 단순화를 위해 `User.toResponse()`로 양방향 의존을 허용했다. 엄격한 단방향이 필요하다면 별도 `UserMapper`/`UserAssembler` 빈을 두어 Entity가 DTO를 모르게 한다.

## Feature-first 패키징의 장점

| 비교 | Layer-first | Feature-first (현 구현) |
|---|---|---|
| 디렉토리 | `controller/`, `service/`, `entity/` 아래 모든 도메인 혼재 | 도메인별로 모든 계층이 한 곳에 |
| 모듈 분리 | 어려움 — 계층이 도메인을 가로지름 | 쉬움 — `feature/user`만 떼면 됨 |
| 신규 기능 추가 | 4~5개 디렉토리 동시 수정 | `feature/{name}` 하위에서 완결 |
| MSA 마이그레이션 | 재구조화 필요 | 그대로 분리 가능 |

## Entity ↔ DTO 변환

```java
// User.java
public UserDTO.Response toResponse() {
    return UserDTO.Response.builder()
            .id(id)
            .name(name)
            ...
            .build();
}

// UserService.java
return user.toResponse();   // Service는 항상 DTO를 반환
```

**원칙**: Controller는 Entity를 절대 보지 않는다. Service가 변환 책임을 진다.

## 주의사항

- **공통 컴포넌트의 경계**: `common/`은 도메인 비의존이어야 한다. `common/component/mask`가 `feature/user`를 import하면 순환 의존 위험.
- **DTO의 마스킹은 Response 전용**: 입력 DTO(`Request`)는 마스킹하지 않는다 — 요청은 원본 데이터 그대로 받아야 함.
- **Entity의 불변성**: `User`는 모든 필드를 `final`로 두고 `@Builder`로 생성. 가변 도메인(JPA Entity 등)이라면 생성/수정 메서드를 명시적으로 노출하는 게 안전.
