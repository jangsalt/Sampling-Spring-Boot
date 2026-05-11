# Spring Dashboard에서 Gradle 프로젝트가 보이지 않을 때

## 증상

Maven에서 Gradle로 전환한 후 VSCode의 **Spring Boot Dashboard**(Spring Boot Tools 확장)에 프로젝트가 사라진다.

## 실제 발견된 원인 (본 프로젝트 케이스)

본 프로젝트에서 Spring Dashboard가 보이지 않은 직접적 원인은 다음 세 가지였다:

1. **`.vscode/settings.json`의 `java.configuration.updateBuildConfiguration: "interactive"`**
   - 빌드 파일(pom.xml↔build.gradle)이 바뀌어도 VSCode가 자동으로 프로젝트를 재import 하지 않고 사용자에게 매번 묻는다. 프롬프트를 놓치거나 무시하면 빌드 시스템 변경이 반영되지 않는다.

2. **`target/` 디렉토리 잔존** — Maven 빌드 산출물 디렉토리. JDT.LS가 이 디렉토리를 보고 여전히 Maven 프로젝트로 인식할 수 있다.

3. **`java.import.gradle.enabled` 미명시 / `java.import.maven.enabled: true` 기본값** — 두 빌드 시스템 import가 모두 켜져 있어 우선순위 충돌 가능.

위 셋을 정리하면 대부분 해결된다. 만약 그래도 안 보이면 아래 "원인" 섹션과 체크리스트로 진행.

## 원인

Spring Boot Dashboard는 다음을 기준으로 Spring Boot 앱을 탐지한다:

| 빌드 도구 | 탐지 기준 |
|---|---|
| Maven | `pom.xml`에 `spring-boot-starter-parent` 또는 `spring-boot-dependencies` BOM |
| **Gradle** | `build.gradle`에 `org.springframework.boot` 플러그인 |

탐지 자체는 Java Language Server(Eclipse JDT.LS) + Gradle Tooling API를 통해 이루어진다. 따라서 빌드 시스템이 바뀌었는데도 **Java/Gradle 확장이 프로젝트를 재import 하지 않으면** Spring Dashboard도 갱신되지 않는다.

또한 Spring Dashboard는 **메인 클래스**를 명시하면 더 안정적으로 인식한다. 기본은 `@SpringBootApplication` 자동 탐색이지만, 모듈/멀티프로젝트 환경에선 누락될 수 있다.

## 수정 사항

### 1. `build.gradle` 보완

`springBoot` 블록을 추가하여 메인 클래스를 명시하고 빌드 정보를 생성한다.

```gradle
springBoot {
    mainClass = 'me.jangsalt.sampling.SamplingApplication'
    buildInfo()
}
```

**역할:**
- `mainClass` — Spring Dashboard가 부트스트랩 클래스를 명시적으로 인식.
- `buildInfo()` — `META-INF/build-info.properties` 자동 생성. Actuator의 `/actuator/info`에서 노출되고, Dashboard가 앱 메타데이터를 표시할 때 활용.

전체 `build.gradle`은 [/build.gradle](../../build.gradle) 참고.

### 2. `.vscode/settings.json` 보정

본 프로젝트의 현재 설정:
```json
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "java.import.gradle.enabled": true,
    "java.import.gradle.wrapper.enabled": true,
    "java.import.maven.enabled": false,
    "spring-boot.ls.problem.boot2.JAVA_VERSION": "WARNING"
}
```

| 키 | 의미 |
|---|---|
| `updateBuildConfiguration: automatic` | build.gradle 변경 시 묻지 않고 자동 재import |
| `java.import.gradle.enabled: true` | Gradle import 활성 (기본값이지만 명시) |
| `java.import.gradle.wrapper.enabled: true` | Gradle 시스템 미설치 시에도 wrapper로 임포트 |
| `java.import.maven.enabled: false` | Maven import 비활성 — 충돌 방지 |

### 3. Maven 잔존물 제거

```bash
rm -rf target/
```

`target/`이 남아있으면 JDT.LS가 Maven 산출물로 오해할 수 있다. 이미 Maven 파일(`pom.xml`, `mvnw`, `.mvn/`)은 제거됐어도 빌드 결과물 디렉토리는 별도로 지워야 한다.

### 4. VSCode에서 Gradle 프로젝트 재import

`build.gradle`만 수정해도 IDE 캐시 때문에 인식되지 않을 수 있다. 아래 순서로 재로드한다.

#### A. Gradle 프로젝트 새로고침 (가장 먼저 시도)

1. `build.gradle` 파일을 열고 우클릭
2. **"Reload Gradle Project"** 선택 (또는 명령 팔레트에서 `Gradle: Refresh Gradle Project`)

#### B. Java 워크스페이스 클린 (위로 해결 안 될 때)

1. `Cmd+Shift+P` → **"Java: Clean Workspace"**
2. "Reload and Delete" 또는 "Restart and Delete" 선택
3. Language Server가 재시작되며 모든 프로젝트를 다시 스캔

#### C. VSCode 자체 재시작 (최후 수단)

1. `Cmd+Shift+P` → **"Developer: Reload Window"**
2. 또는 VSCode 완전 종료 후 재실행

### 3. 필요한 VSCode 확장 확인

Spring Boot Dashboard가 동작하려면 다음 확장이 모두 활성화되어야 한다:

| 확장 | 역할 |
|---|---|
| **Extension Pack for Java** (`vscjava.vscode-java-pack`) | Java Language Server, JDT |
| **Gradle for Java** (`vscjava.vscode-gradle`) | Gradle 통합 |
| **Spring Boot Extension Pack** (`vmware.vscode-boot-dev-pack`) | Spring Dashboard 포함 |
| **Spring Boot Tools** (`vmware.vscode-spring-boot`) | `@SpringBootApplication` 인식 |
| **Spring Boot Dashboard** (`vscjava.vscode-spring-boot-dashboard`) | 좌측 사이드바 앱 목록 |

설치 확인:
```
Cmd+Shift+X → 위 확장명 검색 → 모두 Installed/Enabled 상태인지 확인
```

## 검증

재로드 후:
1. 좌측 사이드바에서 **Spring Boot Dashboard** 아이콘 클릭 (잎사귀 모양)
2. `APPS` 섹션에 `sampling`(또는 프로젝트명) 항목이 표시되어야 함
3. 항목 우클릭 → `Start` 가능, 실행 시 디버그/콘솔에 출력

CLI에서 확인:
```bash
./gradlew tasks --all | grep -E "bootRun|bootJar|bootBuildInfo"
```
다음이 보여야 정상:
```
bootBuildInfo
bootJar
bootRun
```

## 트러블슈팅 체크리스트

문제가 지속될 때 다음을 순서대로 확인:

- [ ] `build.gradle`에 `id 'org.springframework.boot'` 플러그인이 있는가
- [ ] `build.gradle`에 `springBoot { mainClass = '...' }` 가 있는가
- [ ] `./gradlew build` 가 성공하는가 (터미널에서)
- [ ] `./gradlew bootRun` 으로 실행되는가
- [ ] VSCode 좌측 하단의 Gradle 아이콘에 빌드 진행 표시가 나타났는가
- [ ] `.vscode/settings.json`에 `java.import.gradle.enabled: false` 같은 비활성 설정이 없는가
- [ ] `Output` 패널 → `Gradle for Java` 에 import 에러가 없는가
- [ ] `Output` 패널 → `Language Support for Java` 에 프로젝트 스캔 로그가 있는가

## 자주 만나는 추가 이슈

### 이슈: `.classpath`, `.project` 등 Eclipse 메타가 남아있음
Maven에서 Gradle로 전환 시 이전 Eclipse 메타데이터가 충돌할 수 있다.
```bash
rm -rf .classpath .project .settings/ bin/
```
삭제 후 VSCode 재로드.

### 이슈: 다른 JDK 버전이 자동 선택됨
`build.gradle`의 toolchain은 21로 설정되어 있지만 IDE가 다른 JDK를 사용할 수 있다.
- `Cmd+Shift+P` → **"Java: Configure Java Runtime"** 에서 프로젝트의 JDK를 21로 명시

### 이슈: bootRun 시 포트 충돌
이전 Maven 실행 프로세스가 살아있을 수 있다.
```bash
lsof -i :20000 -t | xargs kill
```

## 참고

- [VSCode Spring Boot Dashboard GitHub](https://github.com/microsoft/vscode-spring-boot-dashboard)
- [Spring Boot Gradle Plugin Reference](https://docs.spring.io/spring-boot/gradle-plugin/reference/htmlsingle/)
- 본 프로젝트의 build.gradle: [/build.gradle](../../build.gradle)
