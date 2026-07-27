# M1 인증 계약 체크리스트

## 사용 방법

이 문서는 M1 인증 기반 구현의 강제 진입점과 완료 게이트다. 사람과 AI agent는 인증, JWT,
refresh token, 현재 보호자, requestId 또는 오류 처리 작업을 시작하기 전에 이 파일을 끝까지 읽는다.

계약 원본의 우선순위는 다음과 같다.

1. HTTP wire contract: `docs/openapi.yaml`
2. REST 오류와 requestId contract: `docs/error-response.md`
3. M1 범위와 완료 조건: `docs/backend-mvp-plan.md`
4. 제품 제약과 인수 기준: `docs/seed.yaml`

문서가 충돌하면 구현으로 해결하지 않는다. 먼저 문서를 같은 계약으로 수정하고 검증한 뒤 구현한다.
아래 필수 항목과 검증 게이트가 모두 충족되기 전에는 M1을 `완료`로 변경하지 않는다.

## 구현 전 확인

- [ ] `docs/openapi.yaml`의 M1 endpoint와 request/response schema를 읽었다.
- [ ] `docs/error-response.md`의 ProblemDetail, 에러 코드, requestId 규칙을 읽었다.
- [ ] migration, security, token rotation, requestId filter 설계를 사람 개발자가 명시적으로 리뷰했다.
- [ ] 새 package, layer 또는 dependency가 필요하면 필요성과 대안을 먼저 기록했다.

## Token 계약

- [ ] access token TTL은 900초(15분)다.
- [ ] refresh token TTL은 2,592,000초(30일)다.
- [ ] refresh 성공 시 기존 refresh token을 폐기하고 새 access/refresh token 쌍을 발급한다.
- [ ] 회전된 이전 refresh token의 재사용은 `AUTH_REFRESH_INVALID` 401이다.
- [ ] logout은 전달된 refresh token을 폐기하며 같은 token에 대한 재요청은 멱등이다.
- [ ] refresh token 원문은 DB, 애플리케이션 로그, ProblemDetail에 저장하거나 노출하지 않는다.
- [ ] DB에는 검증에 필요한 refresh token hash와 lifecycle 시각만 저장한다.
- [ ] 비밀번호 원문은 저장하거나 로그에 남기지 않고 password hash만 저장한다.

## 보호자 계약

- [ ] `profileType`은 `individual`, `couple`, `family`만 허용한다.
- [ ] `individual`은 `female` 또는 `male` gender가 필수다.
- [ ] `couple`과 `family`는 gender를 받지 않는다.
- [ ] `identityVisibility`는 `public` 또는 `private`만 허용한다.
- [ ] `GET /api/guardians/me`는 id, email, profileType, 조건부 gender, identityVisibility, status를 반환한다.
- [ ] 공개 여부 변경은 다음 추천부터 적용되고 기존 match/chat에는 영향을 주지 않는다.

## ProblemDetail 계약

- [ ] 모든 REST 오류의 Content-Type은 `application/problem+json`이다.
- [ ] 모든 REST 오류는 `type`, `title`, `status`, `detail`, `instance`, `code`, `requestId`를 포함한다.
- [ ] field-level validation 정보가 있을 때만 `fieldErrors`를 포함한다.
- [ ] HTTP status와 ProblemDetail의 `status`가 같다.
- [ ] 앱이 분기하는 안정적인 값은 `detail`이 아니라 `code`다.
- [ ] Spring MVC advice와 Spring Security 401·403이 같은 ProblemDetail serializer를 사용한다.
- [ ] 내부 exception, stack trace, SQL, token, 비밀번호 및 정확한 위치를 오류 응답에 노출하지 않는다.

## RequestId 계약

- [ ] 모든 HTTP 요청마다 서버가 `req-` + 26자 Crockford Base32 ULID를 하나 생성한다.
- [ ] 형식은 canonical ULID 정규식 `^req-[0-7][0-9A-HJKMNP-TV-Z]{25}$`을 만족한다.
- [ ] 클라이언트가 보낸 `X-Request-Id`는 신뢰하거나 재사용하지 않는다.
- [ ] requestId 생성 filter는 Spring Security보다 먼저 실행되어 401·403에서도 같은 ID를 사용할 수 있다.
- [ ] 같은 requestId를 request attribute와 MDC key `requestId`에 저장한다.
- [ ] 모든 HTTP 응답의 `X-Request-Id` header에 같은 값을 반환한다.
- [ ] 오류 응답의 ProblemDetail `requestId`는 응답 header와 같은 값이다.
- [ ] request 처리의 `finally`에서 MDC 값을 제거해 thread 재사용 시 ID가 섞이지 않게 한다.
- [ ] requestId에는 사용자·token·리소스 식별 정보가 들어가지 않는다.
- [ ] WebSocket client frame 처리마다 별도 requestId를 생성하고 `message_error`와 로그에서 같은 값을 사용한다.

## M1 API

- [ ] `POST /api/auth/signup`
- [ ] `POST /api/auth/login`
- [ ] `POST /api/auth/refresh`
- [ ] `POST /api/auth/logout`
- [ ] `GET /api/guardians/me`
- [ ] `PATCH /api/guardians/me`

## 자동 검증 게이트

- [ ] access/refresh token TTL 테스트가 통과한다.
- [ ] refresh token 발급, 회전, 폐기, 재사용 차단 테스트가 통과한다.
- [ ] DB와 로그에 refresh token 원문이 남지 않는 테스트가 통과한다.
- [ ] individual/couple/family와 gender 조건부 validation 테스트가 통과한다.
- [ ] signup/login/refresh/logout/me 정상·실패 API 테스트가 통과한다.
- [ ] MVC validation ProblemDetail 계약 테스트가 통과한다.
- [ ] `AuthenticationEntryPoint` 401 ProblemDetail 계약 테스트가 통과한다.
- [ ] `AccessDeniedHandler` 403 ProblemDetail 계약 테스트가 통과한다.
- [ ] 모든 오류에서 HTTP status, body status, code, requestId, Content-Type을 검증한다.
- [ ] 정상 응답과 오류 응답의 `X-Request-Id` header를 검증한다.
- [ ] requestId 형식, 요청별 유일성, MDC 정리 테스트가 통과한다.
- [ ] `./gradlew check`가 통과한다.
- [ ] M1 endpoint를 짧게 수동 확인했다.

## 완료 게이트

- [ ] 위 필수 항목을 모두 확인했다.
- [ ] OpenAPI와 실제 응답의 차이가 없다.
- [ ] security, authentication, database, transaction 동작을 사람 개발자가 리뷰했다.
- [ ] 검증 결과를 PR template의 Verification과 Checklist에 기록했다.
- [ ] 위 조건을 모두 충족한 뒤에만 `docs/backend-mvp-plan.md`의 M1을 `완료`로 변경한다.
