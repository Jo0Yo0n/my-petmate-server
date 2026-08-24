# M1 인증 계약 체크리스트

이 문서는 M1 인증 기반 작업의 강제 진입점이자 완료 게이트다. 인증, JWT, refresh token, 현재 보호자, 요청 ID 또는 REST 오류 처리를 시작하기 전에
처음부터 끝까지 읽는다.

계약 상세를 이 문서에 복사하지 않는다. 아래 연결 문서에서 기준을 확인하고, 이 문서에서는 설계 검토와 검증 완료 여부만 기록한다.

## 기준 문서

| 확인할 내용              | 기준 문서와 위치                                                                                             |
|--------------------------|--------------------------------------------------------------------------------------------------------------|
| M1 범위·완료·테스트 기준 | [`backend-mvp-plan.md`의 M1](./backend-mvp-plan.md#m1-인증-기반)                                             |
| 인증 API 경로·요청·응답  | [`openapi.yaml`](./openapi.yaml)의 `/api/auth/*`, `/api/guardians/me`                                        |
| 토큰 수명과 회전 정책    | [`openapi.yaml`](./openapi.yaml)의 `info.x-token-policy`와 인증 API 설명                                     |
| 보호자 요청·응답 스키마  | [`openapi.yaml`](./openapi.yaml)의 `components.schemas.*Guardian*`, `*SignupRequest`                         |
| REST 오류 구조와 코드    | [`error-response.md`의 응답 구조](./error-response.md#응답-구조), [오류 코드](./error-response.md#오류-코드) |
| 요청 ID 생성·전달·정리   | [`error-response.md`의 요청 ID](./error-response.md#요청-id)                                                 |
| 제품 제약과 인수 기준    | [`seed.yaml`](./seed.yaml)의 `constraints`, `acceptance_criteria`                                            |

문서가 충돌하면 구현에서 임의로 해석하지 않는다. [`README.md`의 충돌 처리](./README.md#충돌-처리)에 따라 기준 문서를 먼저 일치시킨다.

## 진입 조건

- [ ] 이 문서를 끝까지 읽었다.
- [ ] `openapi.yaml`의 M1 경로와 관련 스키마를 읽었다.
- [ ] `error-response.md`의 오류 구조, 오류 코드, 요청 ID 규칙을 읽었다.
- [ ] migration, 보안 구성, token 회전, 요청 ID filter 설계를 사람 개발자가 명시적으로 검토했다.
- [ ] 새 패키지, 계층 또는 의존성이 필요하면 필요성과 대안을 먼저 기록했다.

진입 조건을 충족하기 전에는 M1 구현을 시작하지 않는다.

## 구현 순서

### 1. 데이터와 validation

- [ ] Guardian·RefreshToken Flyway migration
- [ ] enum과 요청 validation 모델
- [ ] Guardian·RefreshToken entity·repository
- [ ] enum DB 매핑, email 조회, token hash 조회, token 수명 주기 저장

### 2. 비밀번호와 token

- [ ] 비밀번호 해시 저장과 원문 노출 방지
- [ ] JWT access token 발급·검증
- [ ] refresh token 발급·hash 저장
- [ ] refresh 시 기존 token 폐기와 새 token 쌍 발급
- [ ] 폐기·회전된 token 재사용 차단
- [ ] 멱등 logout
- [ ] token 회전의 트랜잭션·동시 요청 정책 확인

### 3. 요청 ID와 REST 오류

- [ ] Spring Security보다 먼저 실행되는 요청 ID filter
- [ ] request attribute, MDC, 응답 header, `ProblemDetail`에 같은 요청 ID 전달
- [ ] 요청 처리 후 `finally`에서 MDC 정리
- [ ] 공통 `ProblemDetail` 생성·직렬화
- [ ] Bean Validation, JSON·enum·query·path 오류 처리
- [ ] 리소스 없음, 중복, 상태 충돌, 예상하지 못한 오류 처리
- [ ] MVC와 Spring Security 401·403이 같은 오류 생성 방식을 사용

### 4. 인증과 보호자 API

- [ ] JWT authentication filter와 `SecurityFilterChain`
- [ ] 인증 제외·보호 경로 설정
- [ ] JWT subject에서 현재 Guardian 복원
- [ ] signup, login, refresh, logout
- [ ] `/api/guardians/me` 조회·수정
- [ ] 보호자 유형·성별·공개 여부의 조건부 validation

### 5. 테스트와 프론트 연결 준비

- [ ] API·보안 자동 테스트
- [ ] OpenAPI 타입 생성 가능 여부 확인
- [ ] 프론트의 `application/problem+json` 파싱 계약 확인
- [ ] 동시 401에서 refresh 한 번, 원 요청별 재시도 한 번 정책 확인

## 보안 검토 항목

- [ ] 비밀번호와 refresh token 원문이 DB에 저장되지 않는다.
- [ ] 비밀번호, token, 내부 예외, stack trace, SQL이 로그나 오류 응답에 노출되지 않는다.
- [ ] refresh token의 만료·폐기·회전·재사용 상태를 구분해 안전하게 처리한다.
- [ ] logout 재요청이 안전하게 성공한다.
- [ ] 인증 실패와 권한 부족을 구분한다.
- [ ] 요청 ID는 사용자·token·리소스 정보를 포함하지 않으며 인증이나 멱등성 키로 사용하지 않는다.

구체적인 TTL, 허용 enum, 비밀번호 패턴, 오류 필드, 요청 ID 정규식은 기준 문서에서만 관리한다.

## 검증 게이트

### 자동 검증

- [ ] access·refresh token TTL 테스트
- [ ] refresh token 발급·회전·폐기·재사용 차단 테스트
- [ ] DB와 로그의 비밀번호·refresh token 원문 미노출 테스트
- [ ] 보호자 유형과 성별의 조건부 validation 테스트
- [ ] signup·login·refresh·logout·현재 보호자 정상·실패 API 테스트
- [ ] MVC validation `ProblemDetail` 계약 테스트
- [ ] `AuthenticationEntryPoint` 401 계약 테스트
- [ ] `AccessDeniedHandler` 403 계약 테스트
- [ ] 모든 오류의 HTTP status, body status, code, 요청 ID, Content-Type 테스트
- [ ] 정상·오류 응답의 `X-Request-Id` 테스트
- [ ] 요청 ID 형식·요청별 유일성·MDC 정리 테스트
- [ ] `./gradlew check` 통과

### 수동 검증과 검토

- [ ] M1 API의 주요 정상·실패 흐름을 짧게 확인했다.
- [ ] OpenAPI와 실제 요청·응답의 차이가 없다.
- [ ] 보안, 인증, 데이터베이스, 트랜잭션 동작을 사람 개발자가 검토했다.
- [ ] 검증 결과를 Pull Request의 `Verification`과 `Checklist`에 기록했다.

## 완료 게이트

- [ ] 진입 조건을 모두 충족했다.
- [ ] 구현 순서의 항목을 모두 완료했다.
- [ ] 보안 검토 항목을 모두 확인했다.
- [ ] 자동·수동 검증을 모두 통과했다.
- [ ] 위 조건을 충족한 뒤에만 [`backend-mvp-plan.md`](./backend-mvp-plan.md#진행-현황)의 M1을 `완료`로 변경한다.
