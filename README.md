# samplingBySpringBoot

Spring Boot 학습용 샘플 프로젝트. **DTO 응답 자동 마스킹**을 중심으로 커스텀 어노테이션, Jackson 커스텀 시리얼라이저, 요청 스코프 컨텍스트, 계층형 아키텍처 패턴을 시연한다.

## 기술 스택

| 항목 | 버전 / 비고 |
|---|---|
| Spring Boot | 3.5.14 (CVE-2026-40972 패치 포함) |
| Java | 21 |
| Build | Gradle 8.14.4 (gradlew 포함) |
| Lombok | `@Getter` / `@Builder` / `@RequiredArgsConstructor` 등 |
| Jackson | 2.x (Spring Boot 3.5 기본) |
| 저장소 | In-memory `LinkedHashMap` (예제 목적) |

## 핵심 기능

- **`@Mask` 어노테이션**: DTO 필드에 부착하여 JSON 직렬화 시점에 자동 마스킹
- **마스킹 타입 6종**: `DEFAULT`, `NAME`, `EMAIL`, `PHONE`, `RRN`, `CARD`
- **요청별 마스킹 토글**: `?mask=false` 쿼리로 원본 노출 (기본은 마스킹)
- **사용자 CRUD + 검색**: 이름/이메일 부분 검색, 12명 샘플 데이터 자동 적재

## 디렉토리 구조

```
src/main/java/me/jangsalt/sampling/
├── SamplingApplication.java
├── common/
│   ├── component/
│   │   └── mask/                       # 마스킹 컴포넌트
│   │       ├── Mask.java               # 커스텀 어노테이션
│   │       ├── MaskType.java           # 마스킹 유형 enum
│   │       ├── MaskingUtil.java        # 마스킹 로직
│   │       ├── MaskSerializer.java     # Jackson 시리얼라이저
│   │       ├── MaskingContext.java     # ThreadLocal 컨텍스트
│   │       └── MaskingInterceptor.java # 요청 토글 인터셉터
│   └── config/
│       └── WebConfig.java              # 인터셉터 등록
└── feature/
    └── user/
        ├── controller/UserController.java
        ├── service/UserService.java
        ├── entity/User.java
        └── dto/UserDTO.java            # Request / Search / Response 내부 클래스
```

## API

| Method | 경로 | 설명 |
|---|---|---|
| `GET` | `/api/users` | 전체 조회 (검색 조건 없을 때) |
| `GET` | `/api/users?name=&email=` | 부분 검색 (대소문자 무시, AND) |
| `GET` | `/api/users/{id}` | 단건 조회, 없으면 404 |
| `POST` | `/api/users` | 신규 등록 (Body: `UserDTO.Request`) |
| 공통 | `?mask=false` | 응답 마스킹 해제 |

기본 서버 포트: `20000` ([application.yml](src/main/resources/application.yml))

## 실행

```bash
./gradlew bootRun
```

빌드만 (테스트 제외):
```bash
./gradlew build -x test
```

원본 데이터를 보려면:
```bash
curl "http://localhost:20000/api/users/1?mask=false"
```

## 기술 상세 문서

세부 패턴/구현은 [docs/skills/](docs/skills/README.md)를 참고.
