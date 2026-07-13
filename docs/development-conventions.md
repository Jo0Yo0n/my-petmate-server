# 개발 컨벤션

이 규칙은 프로젝트 계약의 일부다. 사람 개발자와 AI agent는 코드나 빌드 설정을 변경하기 전에 이 파일을 읽어야 한다.

## 범위

- 이 저장소는 백엔드 전용이다.
- production code의 대부분은 사람 개발자가 작성한다.
- AI는 스캐폴딩, 문서 작성, 리뷰, 오류 설명, 요청받은 작은 독립 snippet 제안을 할 수 있다.
- AI는 명시적 승인 없이 비즈니스 로직을 구현하면 안 된다.

## 백엔드 스택

- Java: 21 LTS
- Framework: Spring Boot 3.x
- Build: Gradle Kotlin DSL
- Database: MVP에서는 PostgreSQL만 사용
- Migration: Flyway
- Formatting: Spotless with Google Java Format

## 코드 규칙

- 첫 구현은 지루하고 feature 안에 국소적으로 유지한다.
- 구체적인 필요가 생기기 전에는 package, layer, factory, interface를 추가하지 않는다.
- security, transaction, persistence 동작은 명시적 리뷰 없이 변경하지 않는다.
- API 경계에서 입력을 검증한다.
- 다른 package에서 실제로 필요하지 않다면 type과 method는 package-private을 우선한다.
- 코드 변경을 넘기기 전에 `./gradlew spotlessApply`를 실행한다.
- PR을 열기 전에 `./gradlew check`를 실행한다.

## 커밋 규칙

영어 Conventional Commits를 사용한다.

```text
<type>: <short summary>
```

허용 type:

- `feat`
- `fix`
- `docs`
- `test`
- `refactor`
- `chore`
- `build`
- `ci`

예시:

```text
feat: add dog profile registration
fix: exclude blocked guardians from recommendations
docs: add backend milestone plan
```

## 브랜치 규칙

다음 형식을 사용한다.

```text
<type>/<short-kebab-summary>
```

예시:

```text
feat/dog-profile-registration
fix/block-recommendation-filter
docs/backend-mvp-plan
```

## Pull Request 규칙

모든 PR은 `.github/pull_request_template.md`를 사용해야 한다.

검증 섹션에는 실행한 명령과 수동 API 확인 내용을 적는다. security, authentication, database schema, persistence, transaction 동작이 변경되었다면 체크리스트에 명시한다.

AI가 코드를 생성하거나 수정했다면 무엇이 바뀌었고 사람 개발자가 어떻게 리뷰했는지 적는다.
