# 개발 규칙

이 문서는 코드·빌드 설정·Git 작업 방식을 정하는 기준 문서다. 기능 요구사항과 API 계약의 위치는 [
`documentation-guide.md`](./documentation-guide.md)에서 확인한다.

코드·문서·빌드 설정을 변경하거나 Git 작업을 하기 전에 사람 개발자와 AI 에이전트 모두 이 문서를 읽어야 한다.

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
- 코드·빌드 설정 변경의 Pull Request를 열기 전에 `./gradlew check`를 실행한다. 문서 전용 변경의 검증은 [리뷰와 검증](#리뷰와-검증)을 따른다.

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

브랜치 이름은 다음 형식을 사용한다. milestone에는 변경이 속한 `m0`~`m7`을 소문자로 적고, summary는 영어 소문자·숫자·하이픈으로 짧고 구체적으로 작성한다.

```text
<type>/<milestone>-<short-kebab-summary>
```

예시:

```text
feat/m2-dog-profile-registration
fix/m3-block-recommendation-filter
docs/m1-reorganize-backend-docs
```

- 관련 작업 브랜치가 있으면 이어서 사용한다. 새 브랜치는 최신 `main`을 기준으로 만들고 기본 브랜치에 직접 커밋하지 않는다.
- 브랜치 작업과 커밋 전에는 현재 브랜치·변경 범위를 확인하고 기존 사용자 변경을 보존한다.
- Issue 번호는 브랜치명에 강제하지 않고 PR 제목과 본문에 명시한다.

### AI의 Git 작업 권한

- 문서·코드 수정 요청은 커밋·push·PR 생성 요청으로 해석하지 않는다.
- 커밋·push·PR 생성은 사용자가 요청한 범위에서 수행한다. PR 생성 요청에는 해당 PR에 필요한 작업 브랜치·커밋·push가 포함된다.
- PR 생성 권한은 병합 권한을 포함하지 않는다.

## Pull Request

모든 Pull Request는 [저장소 템플릿](../.github/pull_request_template.md)을 사용한다.

- PR 제목은 영어 Conventional Commits 형식을 사용하고, 관련 Issue가 있으면 `docs: clarify collaboration workflow (Issue #12)`처럼 번호를 반드시 적는다.
- 다른 저장소의 Issue는 PR 제목에 `(Issue Jo0Yo0n/my-petmate-frontend#12)`처럼 `Issue`와 저장소 전체 이름을 함께 적는다.
- PR 본문에도 관련 Issue를 명시한다. 다른 저장소의 Issue는 `Closes 소유자/저장소#번호` 또는 `Refs 소유자/저장소#번호`처럼 참조한다.
- Issue의 완료 조건을 모두 충족하는 PR만 본문에 `Closes #12`를 사용한다. 부분 구현·선행 작업은 `Refs #12`와 남은 범위를 적는다.
- Issue 없는 PR은 제목에서 번호를 생략하고 본문에 사유를 적는다. 번호를 임의로 만들지 않는다.
- 하나의 PR은 하나의 설명 가능한 변경을 담는다. 다른 저장소의 선행·연관 Issue와 PR을 링크한다.

- `검증`에는 실행한 명령과 수동 API 확인 결과를 기록한다.
- 보안, 인증, 데이터베이스 스키마, 영속성, 트랜잭션 동작이 바뀌었다면 `체크리스트`에 명시한다.
- AI가 코드를 생성하거나 수정했다면 변경 범위와 사람 개발자의 검토 방법을 기록한다.

## Issue와 작업 범위

- 기능·버그·여러 단계에 걸친 변경은 기존 관련 Issue를 먼저 확인한다. Issue에는 배경, 범위, 완료 조건, 검증 계획을 기록한다.
- 단순 오탈자·링크 수정은 Issue 없이 PR로 설명할 수 있다. Issue가 없는 PR에는 사유를 적는다.
- 두 저장소의 변경이 필요하면 저장소별 Issue를 상호 링크하고 선행 관계를 적는다. 계약 상세는 담당 문서를 링크한다.
- AI는 원격 Issue 생성·수정을 사용자가 요청한 범위에서만 수행한다. 중복 Issue를 만들지 않는다.

## 리뷰와 검증

- 실행한 검사·결과, 미실행 검사·이유, 필요한 수동 확인을 PR에 구분해 적는다. 기존 완료 기록을 이번 실행 결과로 보고하지 않는다.
- 작성자의 자체 검토와 다른 사람의 승인 리뷰를 구분한다. AI 검토를 사람의 검토로 대신하지 않는다.
- 문서만 변경하면 변경한 링크·앵커·기준 문서 일치와 공백을 확인한다.
- 코드·빌드 변경은 저장소의 검사와 변경에 필요한 수동 검증을 수행한다.
- CI 검사가 구성되어 있으면 해당 결과를 확인한다. 로컬 검사 결과를 CI 통과로 표현하지 않는다.

## 병합

- 리뷰 지적, 필요한 검사, 계약 충돌을 해결하고 완료 조건을 확인한 뒤 병합한다.
- AI는 사용자의 명시적인 병합 요청 없이 병합하지 않는다.
- 기본 브랜치에 병합한 뒤 연결된 Issue의 실제 종료 여부를 확인한다. 자동 종료 설정과 병합 대상 브랜치에 따라 결과가 다를 수 있다.
- 미충족 완료 조건이 있으면 Issue를 완료 처리하지 않는다. PR 병합만으로 마일스톤·검증 게이트 전체를 완료로 바꾸지 않는다.

## Projects

- 두 저장소의 작업은 공통 Project에서 관리한다. 상태는 `Todo → In Progress → Done`을 사용한다.
- Issue를 기본 보드 항목으로 사용하고 관련 PR을 연결한다. Issue 없는 작은 수정은 PR을 보드 항목으로 사용할 수 있다.
- 작업 착수 시 `In Progress`, 병합과 Issue 완료 조건 충족 후 `Done`으로 갱신한다. 검토 중에도 `In Progress`를 유지한다.
- 마일스톤·계약 준비 상태는 담당 문서를 링크하고 보드에 복사하지 않는다.
- 상태 자동화는 실제 Project 설정을 확인한 경우에만 사용한다. AI의 Project 생성·항목·상태 변경은 사용자가 요청한 범위에서 수행한다.

## 변경 전 확인 순서

1. [`documentation-guide.md`](./documentation-guide.md)에서 작업에 필요한 기준 문서를 찾는다.
2. [`backend-mvp-plan.md`](./backend-mvp-plan.md#진행-현황)에서 현재 마일스톤을 확인한다.
3. 인증, JWT, refresh token, 현재 보호자, 요청 ID, 오류 처리 작업이면 [
   `m1-auth-contract-checklist.md`](./m1-auth-contract-checklist.md)를 끝까지 읽는다.
4. 보안, 트랜잭션, 영속성 변경은 구현 전에 사람 개발자의 명시적인 검토를 받는다.
