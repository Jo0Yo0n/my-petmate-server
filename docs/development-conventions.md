# 개발 규칙

이 문서는 코드·빌드 설정·Git 작업 방식을 정하는 기준 문서다. 기능 요구사항과 API 계약의 위치는 [`documentation-guide.md`](./documentation-guide.md)에서 확인한다.

코드나 빌드 설정을 변경하기 전에 사람 개발자와 AI 에이전트 모두 이 문서를 읽어야 한다.

## 역할과 범위

- 이 저장소는 백엔드 전용이다.
- 프로덕션 코드의 대부분은 사람 개발자가 작성한다.
- AI는 스캐폴딩, 문서 작성, 코드 검토, 오류 설명, 명시적으로 요청받은 작은 독립 코드 조각 제안을 할 수 있다.
- AI는 명시적인 승인 없이 비즈니스 로직을 구현하지 않는다.
- 보안, 트랜잭션, 영속성 동작은 사람 개발자의 명시적인 검토 없이 변경하지 않는다.

## 기술 구성

| 구분         | 기준                          |
|--------------|-------------------------------|
| Java         | 21 LTS                        |
| 프레임워크   | Spring Boot 3.x               |
| 빌드         | Gradle Kotlin DSL             |
| 데이터베이스 | MVP에서는 PostgreSQL만 사용   |
| 마이그레이션 | Flyway                        |
| 코드 형식    | Spotless와 Google Java Format |

## 코드와 설계

- 첫 구현은 단순하고 명시적으로 작성하며 해당 기능 안에 국소적으로 둔다.
- 구체적인 필요가 생기기 전에는 패키지, 계층, 팩터리, 인터페이스를 추가하지 않는다.
- 새 추상화나 의존성이 필요하면 먼저 필요성, 대안, 영향을 기록한다.
- API 경계에서 입력값을 검증한다.
- 다른 패키지에서 실제로 사용하지 않는 타입과 메서드는 패키지 전용 접근을 우선한다.
- 코드 변경을 넘기기 전에 `./gradlew spotlessApply`를 실행한다.
- Pull Request를 열기 전에 `./gradlew check`를 실행한다.

## Git 작업

### 커밋

커밋 메시지는 영어 Conventional Commits 형식을 사용한다.

```text
<type>: <short summary>
```

허용하는 `type`은 다음과 같다.

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
docs: reorganize backend documentation
```

### 브랜치

브랜치 이름은 다음 형식을 사용한다.

```text
<type>/<short-kebab-summary>
```

예시:

```text
feat/dog-profile-registration
fix/block-recommendation-filter
docs/reorganize-backend-docs
```

## Pull Request

모든 Pull Request는 `.github/pull_request_template.md`를 사용한다.

- `Verification`에는 실행한 명령과 수동 API 확인 결과를 기록한다.
- 보안, 인증, 데이터베이스 스키마, 영속성, 트랜잭션 동작이 바뀌었다면 `Checklist`에 명시한다.
- AI가 코드를 생성하거나 수정했다면 변경 범위와 사람 개발자의 검토 방법을 기록한다.

## 변경 전 확인 순서

1. [`documentation-guide.md`](./documentation-guide.md)에서 작업에 필요한 기준 문서를 찾는다.
2. [`backend-mvp-plan.md`](./backend-mvp-plan.md#진행-현황)에서 현재 마일스톤을 확인한다.
3. 인증, JWT, refresh token, 현재 보호자, 요청 ID, 오류 처리 작업이면 [
   `m1-auth-contract-checklist.md`](./m1-auth-contract-checklist.md)를 끝까지 읽는다.
4. 보안, 트랜잭션, 영속성 변경은 구현 전에 사람 개발자의 명시적인 검토를 받는다.
