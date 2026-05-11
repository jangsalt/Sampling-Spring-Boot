# Skills Index

본 프로젝트에서 활용한 기술 패턴을 **카테고리별**로 정리한다. 각 문서는 *왜 / 어디서 / 어떻게*를 중심으로 코드 위치와 함께 설명한다.

## 카테고리 구조

```
docs/skills/
├── jackson/        # JSON 직렬화/역직렬화 관련
├── spring-mvc/     # Spring MVC 웹 계층
└── pattern/        # 언어/아키텍처 패턴 (도메인 무관)
```

## 문서 목록

### [`jackson/`](jackson/) — JSON 직렬화 커스터마이징
| 문서 | 내용 |
|---|---|
| [custom-annotation](jackson/custom-annotation.md) | `@Mask` 커스텀 어노테이션 + `@JacksonAnnotationsInside` 메타 어노테이션 패턴 |
| [custom-serializer](jackson/custom-serializer.md) | `JsonSerializer` + `ContextualSerializer` 구현, 필드별 동적 시리얼라이저 |
| [version-migration](jackson/version-migration.md) | Spring Boot 3.5 ↔ 4.0 (Jackson 2 ↔ 3) API 차이와 이행 방법 |

### [`spring-mvc/`](spring-mvc/) — Spring 웹 계층
| 문서 | 내용 |
|---|---|
| [handler-interceptor](spring-mvc/handler-interceptor.md) | `HandlerInterceptor` 라이프사이클, `WebMvcConfigurer` 등록 |
| [modelattribute-binding](spring-mvc/modelattribute-binding.md) | `@ModelAttribute`로 쿼리 파라미터를 DTO에 자동 바인딩 |
| [exception-handling](spring-mvc/exception-handling.md) | `@RestControllerAdvice` 2단 분리, `BusinessException`/`ErrorCode` 패턴 |
| [common-response](spring-mvc/common-response.md) | `ResponseBodyAdvice`로 모든 응답을 `CommonResponse` envelope로 통일 |

### [`pattern/`](pattern/) — 언어/아키텍처 패턴
| 문서 | 내용 |
|---|---|
| [threadlocal-context](pattern/threadlocal-context.md) | 요청 스코프 상태 보관, 누수 방지 |
| [layered-architecture](pattern/layered-architecture.md) | Feature-first 패키지 + Controller/Service/Entity/DTO 분리 |
| [inner-class-dto](pattern/inner-class-dto.md) | DTO 내부 클래스(`Request`/`Search`/`Response`) 그룹화 |

## 읽기 순서 추천

마스킹 기능 구현 흐름을 따라가기:

1. [jackson/custom-annotation](jackson/custom-annotation.md) — `@Mask` 어노테이션 설계
2. [jackson/custom-serializer](jackson/custom-serializer.md) — 시리얼라이저 동작
3. [pattern/threadlocal-context](pattern/threadlocal-context.md) — 요청별 토글 저장소
4. [spring-mvc/handler-interceptor](spring-mvc/handler-interceptor.md) — 요청 단위 토글 적용
5. [pattern/layered-architecture](pattern/layered-architecture.md) → [pattern/inner-class-dto](pattern/inner-class-dto.md) → [spring-mvc/modelattribute-binding](spring-mvc/modelattribute-binding.md) — 사용자 도메인 구성
6. [jackson/version-migration](jackson/version-migration.md) — 향후 Spring Boot 4 업그레이드 참고

## 새 문서 추가 규칙

새로운 기능을 추가했다면:

1. **카테고리 선택**: 적합한 폴더에 배치 (없다면 새 폴더 생성)
   - JSON/직렬화 관련 → `jackson/`
   - Spring MVC 훅/바인딩/필터 → `spring-mvc/`
   - 보안 → `security/` (신규)
   - 영속성 → `persistence/` (신규)
   - 언어/아키텍처 → `pattern/`
2. **파일명**: kebab-case, 주제어 중심 (예: `request-validation.md`). 번호 prefix는 사용하지 않는다 — 순서는 카테고리 README가 책임진다.
3. **이 인덱스 갱신**: 해당 카테고리 표에 한 줄 추가
4. **상호 참조**: 다른 카테고리 문서를 가리킬 때는 `../카테고리/파일.md` 형태로 작성
5. **소스 링크**: 카테고리 하위 문서는 `../../../src/main/...` (`..` 3단계)

## 문서 작성 템플릿

```markdown
# {제목}

## 개요
(왜 이 기술/패턴이 필요한가)

## 위치
- [코드 경로](../../../src/main/...)

## 핵심 코드
(주석 + ★ 핵심 표시)

## 비교 / 대안
(테이블로 트레이드오프)

## 주의사항
(흔한 함정)
```
