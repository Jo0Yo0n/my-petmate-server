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

문서가 충돌하면 구현에서 임의로 해석하지 않는다. [`documentation-guide.md`의 충돌 처리](./documentation-guide.md#충돌-처리)에 따라 기준
문서를 먼저 일치시킨다.

## 설계 검토 결과

2026-08-24에 사람 개발자가 다음 M1 설계를 검토하고 승인했다. REST wire format과 token·email 정책의 상세 값은
[`openapi.yaml`](./openapi.yaml), 오류와 요청 ID의 상세 규칙은 [`error-response.md`](./error-response.md)를 기준으로
한다.

| 주제              | 승인한 결정                                                                                                                                                                                                                                                                              |
|-------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 영속성            | `Guardian`과 `RefreshToken` JPA entity를 직접 매핑한다. UUID는 애플리케이션에서 생성하고 시간은 `Instant`와 주입 가능한 `Clock`으로 계산한다. 범용 base entity나 별도 계층은 추가하지 않는다. 기존 V1 migration은 수정하지 않고 email 정책에 필요한 DB 제약은 V2 migration으로 추가한다. |
| email             | 회원가입과 로그인 입력을 trim한 뒤 `Locale.ROOT` 기준 소문자로 정규화하고 저장·응답에도 같은 값을 사용한다. DB에서도 대소문자를 무시한 유일성을 보장한다. 상세 정책은 `openapi.yaml`의 `info.x-email-policy`를 따른다.                                                                   |
| 비밀번호          | `DelegatingPasswordEncoder`의 BCrypt를 사용하고 초기 cost는 12로 시작한 뒤 실행 환경에서 측정해 조정한다. 원문은 저장하거나 로그에 기록하지 않는다.                                                                                                                                      |
| access token      | Spring Security OAuth2 Resource Server와 Nimbus encoder·decoder를 사용해 HS256 JWT를 발급·검증한다. secret은 외부 설정으로만 주입하고 누락되거나 안전하지 않은 길이면 시작을 실패시킨다. `sub`에는 Guardian UUID만 사용하며 변경 가능한 보호자 정보는 claim에 넣지 않는다.               |
| refresh token     | 256-bit 무작위 opaque token을 padding 없는 Base64 URL 형식으로 반환하고 DB에는 SHA-256 hash만 저장한다. 상세 형식과 수명은 `openapi.yaml`의 `info.x-token-policy`를 따른다.                                                                                                              |
| token 회전        | refresh token row를 pessimistic write lock으로 조회하고 기존 token 폐기와 새 token 저장을 하나의 transaction에서 수행한다. 동시 요청은 먼저 잠근 하나만 성공하며 폐기·회전된 token 재사용은 거부한다. token family와 기기·세션 관리는 M1에 추가하지 않는다.                              |
| logout            | 이미 폐기됐거나 저장되지 않은 refresh token에도 성공을 반환하는 멱등 동작으로 처리한다. access token blacklist는 두지 않고 짧은 TTL 만료를 사용한다.                                                                                                                                     |
| 인증 복원         | stateless Security를 사용하고 JWT `sub`로 매 요청 현재 Guardian을 DB에서 복원한다. `withdrawn`은 인증을 거부하고 `temporarily_restricted`는 제한 대상 기능에서 권한을 검사한다.                                                                                                          |
| 요청 ID           | Security보다 먼저 실행되는 `OncePerRequestFilter`에서 서버 ULID를 생성하고 request attribute, MDC, 응답 header에 전달한다. 처리 후 `finally`에서 MDC의 이전 값을 복원하거나 제거한다.                                                                                                    |
| REST 오류         | MVC advice, `AuthenticationEntryPoint`, `AccessDeniedHandler`가 작은 공통 ProblemDetail factory와 writer를 공유한다. 오류별 범용 exception 계층은 만들지 않는다.                                                                                                                         |
| 입력 email 정규화 | `SignupRequest`와 `LoginRequest`가 같은 null-safe 정규화를 사용하도록 `guardian.support.EmailNormalizer`를 둔다. 각 record에 정규화를 중복하면 정책 변경 시 불일치할 수 있으므로 제외한다. 별도 service나 mapper 계층은 추가하지 않는다.                                                 |
| 검증              | H2 대신 PostgreSQL Testcontainers로 Flyway, DB 제약, repository와 token 회전 동시성을 검증한다.                                                                                                                                                                                          |

### 새 의존성 검토

| 필요 기능      | 승인한 선택과 이유                                                                                          | 제외한 대안의 이유                                                         |
|----------------|-------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| JWT            | Spring Security OAuth2 Resource Server/Jose를 사용해 표준 Bearer token 처리와 Nimbus JWT 검증을 재사용한다. | JWT parsing과 인증 filter 직접 구현은 보안 검증 중복과 오류 가능성이 크다. |
| ULID           | 요청 ID 정규식을 정확히 구현하는 검증된 소형 ULID 라이브러리를 사용한다.                                    | Crockford Base32와 128-bit 범위를 직접 구현하면 계약 오류 위험이 크다.     |
| DB 통합 테스트 | PostgreSQL Testcontainers를 test scope에만 추가한다.                                                        | H2는 PostgreSQL 정규식 제약과 row lock 동작을 동일하게 검증하지 못한다.    |

이 검토는 구현 전 설계 승인이다. 구현 후 보안·인증·데이터베이스·트랜잭션 동작에 대한 사람 검토와 수동 검증은 검증 게이트에서 별도로 수행한다.

## 구현 순서

아래 단계는 위에서 아래로 진행한다. 각 단계의 **완료 조건**을 충족하기 전에는 다음 단계로 넘어가지 않는다. 체크 표시는 코드가 존재할 때가 아니라 명시된 테스트와 검토까지
끝났을 때만 변경한다. 프로덕션 코드는 사람 개발자가 작성하고 AI가 작성한 코드가 있다면 Pull Request에 범위와 검토 방법을 기록한다.

### 1. 기존 입력 모델을 최신 계약에 맞추기

이 단계에서는 이미 작성된 enum과 요청 DTO를 현재 OpenAPI 계약에 맞춘다. entity, service, controller는 아직 작성하지 않는다.

#### 작업

- [x] Guardian enum의 JSON 값과 DB converter가 OpenAPI·V1 migration의 값과 정확히 일치한다.
- [x] signup 비밀번호와 보호자 유형·성별 조합 validation이 요청 DTO에 적용된다.
- [x] `SignupRequest`와 `LoginRequest`가 `info.x-email-policy`에 따라 null-safe하게 email을 정규화한다.
- [x] `RefreshRequest`의 길이와 허용 문자 validation을 `components.schemas.RefreshRequest`와 일치시킨다.
- [x] 누락 필드, 빈 값, 길이 경계, 허용하지 않는 enum과 알 수 없는 JSON 필드의 처리 방식을 계약과 일치시킨다.

#### 필수 테스트

- [x] enum JSON 직렬화·역직렬화와 DB converter 테스트
- [x] 개인·커플·가족의 정상·실패 조합 validation 테스트
- [x] signup 비밀번호 문자군과 길이 경계 테스트
- [x] 대문자와 앞뒤 공백이 있는 email의 정규화 테스트
- [x] 정규화 후 잘못된 email과 길이 경계 테스트
- [x] refresh token의 정상 값과 길이·문자 경계 테스트

#### 완료 조건

- [x] 요청 DTO 테스트가 모두 통과한다.
- [x] DTO의 validation 규칙과 OpenAPI 스키마 사이에 알려진 차이가 없다.
- [x] `./gradlew spotlessApply`와 `./gradlew check`가 통과한다.

### 2. PostgreSQL migration과 통합 테스트 기반 완성

#### 작업

- [x] V1 migration에 Guardian과 RefreshToken table, 관계, enum·수명 주기 제약이 존재한다.
- [x] 기존 V1을 수정하지 않고 email 정규화·대소문자 무시 유일성을 보장하는 V2 migration을 추가한다.
- [x] V2 적용 전 기존 email 처리와 중복 가능성을 검토하고 migration 실패 정책을 정한다.
    - 검토 결과 (2026-08-26): 현재 `guardian` 레코드가 0건이므로 기존 email 정규화·대소문자 중복 데이터 정정은 적용 대상이 없다. V2의 데이터
      충돌 시 정정 정책은 생략한다. 그 밖의 Flyway 실행 실패는 애플리케이션 기동을 중단하고 원인을 수정한 뒤 동일 migration을 재실행한다.
- [x] PostgreSQL Testcontainers와 JUnit 연동 의존성을 test scope에 추가한다.
- [x] 테스트 container에 Flyway migration 전체를 처음부터 적용한다.
- [x] 테스트 profile에서도 Hibernate schema 자동 생성 대신 `ddl-auto=validate`를 사용한다.

#### 필수 테스트

- [x] 빈 PostgreSQL에 V1부터 최신 migration까지 적용되는 테스트
- [x] Guardian의 enum, profile type·gender 조합과 email DB 제약 테스트
- [x] 정규화 전후 대소문자만 다른 email의 중복 거부 테스트
- [x] RefreshToken의 hash, 만료·폐기 시각과 Guardian foreign key 제약 테스트
- [x] Guardian 삭제 시 연결된 RefreshToken 삭제 테스트

#### 완료 조건

- [x] H2 없이 실제 PostgreSQL에서 migration·제약 테스트가 통과한다.
- [x] migration과 OpenAPI email 정책 사이에 알려진 차이가 없다.
- [x] 사람 개발자가 V2 migration과 기존 데이터 영향을 검토했다.
- [x] `./gradlew spotlessApply`와 `./gradlew check`가 통과한다.

### 3. Guardian·RefreshToken 영속성 모델 구현

#### 작업

- [x] 승인된 필드만 사용하는 `Guardian` JPA entity를 작성한다.
- [x] 승인된 필드만 사용하는 `RefreshToken` JPA entity와 Guardian 연관관계를 작성한다.
- [x] UUID는 애플리케이션에서 생성하고 시간은 `Instant`로 저장한다.
- [x] token 만료 계산과 시간 의존 로직을 위해 주입 가능한 `Clock`을 준비한다.
- [x] `GuardianRepository`에 정규화 email 조회와 존재 확인만 추가한다.
- [x] `RefreshTokenRepository`에 hash 조회와 pessimistic write lock 조회를 추가한다.
- [x] 비밀번호 hash와 token hash가 entity의 `toString`, 로그 또는 예외 메시지에 포함되지 않게 한다.
- [x] 범용 base entity, repository interface, mapper 계층과 양방향 연관관계를 추가하지 않는다.

#### 필수 테스트

- [x] Guardian 저장·email 조회·enum 복원 테스트
- [x] 정규화된 email이 Guardian 저장과 email 조회에서 사용하는 유일한 값인지 확인하는 테스트
- [x] RefreshToken 저장·hash 조회·Guardian 연관관계 테스트
- [x] pessimistic write lock이 같은 token row의 동시 변경을 직렬화하는 PostgreSQL 테스트
- [x] 고유 제약 위반이 애플리케이션에서 email 충돌로 식별 가능한지 확인하는 테스트

#### 완료 조건

- [x] repository가 M1 service에 필요한 조회만 노출한다.
- [x] entity mapping이 `ddl-auto=validate`와 모든 PostgreSQL 통합 테스트를 통과한다.
- [x] 사람 개발자가 entity mapping, cascade와 lock 범위를 검토했다.
- [x] `./gradlew spotlessApply`와 `./gradlew check`가 통과한다.

### 4. 비밀번호·access token·refresh token 기본 구성요소 구현

이 단계에서는 token 회전 transaction이나 HTTP endpoint를 만들지 않고 독립적인 생성·검증 구성요소부터 완성한다.

#### 작업

- [ ] `DelegatingPasswordEncoder`의 BCrypt 설정을 추가하고 승인된 초기 cost를 적용한다.
- [ ] BCrypt 검증 시간을 실행 환경에서 측정하고 조정 여부를 기록한다.
- [ ] Spring Security OAuth2 Resource Server/Jose 의존성을 추가한다.
- [ ] HS256 secret을 외부 설정으로만 주입하고 누락·잘못된 길이에서 시작을 실패시킨다.
- [ ] `JwtEncoder`와 `JwtDecoder`가 같은 승인된 issuer, audience, algorithm과 시간 검증 규칙을 사용하게 한다.
- [ ] access token에는 Guardian UUID subject와 token 식별·시간 claim만 넣고 변경 가능한 Guardian 정보는 넣지 않는다.
- [ ] `SecureRandom` 기반 opaque refresh token 생성과 SHA-256 hash 계산을 구현한다.
- [ ] token TTL과 응답 상수는 하나의 application 설정에서 읽고 계약 테스트로 `info.x-token-policy`와 일치함을 고정한다.
- [ ] 테스트가 실제 시스템 시각이나 `sleep`에 의존하지 않도록 `Clock`을 사용한다.

#### 필수 테스트

- [ ] 비밀번호 encode·match·불일치와 원문 미포함 테스트
- [ ] access token subject·issuer·audience·서명·TTL 테스트
- [ ] 만료, 변조, 잘못된 issuer·audience·algorithm token 거부 테스트
- [ ] refresh token 형식·요청별 유일성과 hash 결정성 테스트
- [ ] refresh token 원문과 hash가 서로 다르고 DB 저장 형식과 일치하는지 확인하는 테스트
- [ ] secret 누락·짧은 secret 설정 실패 테스트

#### 완료 조건

- [ ] 암호화 구성요소 단위 테스트가 모두 통과한다.
- [ ] 비밀번호와 token 원문이 로그·예외·영속 객체에 들어가지 않는다.
- [ ] 새 의존성과 설정값의 주입 방법이 README 또는 배포 설정의 지정 위치에 기록돼 있다.
- [ ] `./gradlew spotlessApply`와 `./gradlew check`가 통과한다.

### 5. RefreshToken 수명 주기와 인증 application service 구현

#### 작업

- [ ] signup용 email 중복 확인·비밀번호 hash·Guardian 저장·token 쌍 발급을 하나의 명확한 transaction 경계에 둔다.
- [ ] login은 정규화 email로 Guardian을 조회하고 email 존재 여부와 무관하게 같은 자격 증명 오류를 반환한다.
- [ ] refresh는 token hash row를 pessimistic write lock으로 조회한다.
- [ ] refresh의 기존 token 검증·폐기와 새 token hash 저장을 하나의 transaction에서 처리한다.
- [ ] 만료·폐기·회전·알 수 없는 refresh token을 같은 `AUTH_REFRESH_INVALID` 결과로 변환한다.
- [ ] 동시에 같은 refresh token을 사용하면 먼저 lock을 획득한 요청 하나만 성공하게 한다.
- [ ] logout은 존재하는 token을 폐기하고 이미 폐기됐거나 알 수 없는 token에도 성공하는 멱등 동작으로 만든다.
- [ ] access token blacklist, token family, 기기·세션 관리 기능은 추가하지 않는다.

#### 필수 테스트

- [ ] signup service의 정상 저장, 정규화 email 중복과 rollback 테스트
- [ ] 정규화 전후 email로 login을 요청해도 같은 Guardian을 조회하는 테스트
- [ ] login service의 성공·잘못된 email·잘못된 비밀번호 테스트
- [ ] `withdrawn` login은 `AUTH_INVALID_CREDENTIALS`로 거부하고 `temporarily_restricted` login은 성공하는 테스트
- [ ] refresh 발급·회전·만료·폐기·재사용·알 수 없는 token 테스트
- [ ] 같은 refresh token을 사용하는 두 동시 transaction에서 하나만 성공하는 PostgreSQL 테스트
- [ ] logout 최초·반복·알 수 없는 token 테스트
- [ ] transaction 실패 시 기존 token 폐기와 새 token 저장이 함께 rollback되는 테스트

#### 완료 조건

- [ ] 인증 service 테스트가 정상·실패·동시성·rollback 경로를 모두 통과한다.
- [ ] 사람 개발자가 signup과 refresh의 transaction 경계 및 lock 순서를 검토했다.
- [ ] `./gradlew spotlessApply`와 `./gradlew check`가 통과한다.

### 6. 요청 ID와 공통 REST 오류 기반 구현

Security 401·403도 같은 형식을 사용해야 하므로 실제 API와 SecurityFilterChain보다 이 단계를 먼저 완성한다.

#### 작업

- [ ] 검증된 ULID 라이브러리 의존성을 추가한다.
- [ ] 모든 HTTP 요청에서 새 ID를 만드는 `OncePerRequestFilter`를 구현한다.
- [ ] 클라이언트 `X-Request-Id`를 무시하고 request attribute, MDC, 응답 header에 서버 ID를 전달한다.
- [ ] filter를 Spring Security보다 먼저 실행되도록 등록한다.
- [ ] `finally`에서 MDC의 기존 `requestId`를 복원하거나 제거한다.
- [ ] 오류 code별 안정적인 metadata와 `ProblemDetail`을 만드는 작은 factory를 구현한다.
- [ ] MVC와 Security가 공유하는 `application/problem+json` writer를 구현한다.
- [ ] `ResponseEntityExceptionHandler` 기반 advice에서 Bean Validation, 잘못된 JSON·enum, query·path type
  오류를 변환한다.
- [ ] email 중복, 리소스 없음, 상태 충돌과 예상하지 못한 오류를 계약 code로 변환한다.
- [ ] 내부 exception, stack trace, SQL, email, 비밀번호와 token이 detail·fieldErrors에 노출되지 않게 한다.

#### 필수 테스트

- [ ] 정상·MVC 오류 응답의 `X-Request-Id` 형식과 요청별 유일성 테스트
- [ ] 클라이언트가 보낸 요청 ID를 재사용하지 않는 테스트
- [ ] request attribute, MDC, header와 ProblemDetail requestId 일치 테스트
- [ ] 요청 종료와 filter 예외 후 MDC 복원·정리 테스트
- [ ] Bean Validation, JSON·enum, query·path 오류의 status·code·fieldErrors 테스트
- [ ] email 중복, 리소스 없음, 상태 충돌과 500 오류의 계약 테스트
- [ ] 모든 오류 응답의 Content-Type과 HTTP/body status 일치 테스트

#### 완료 조건

- [ ] MVC 정상·오류 경로의 요청 ID와 ProblemDetail 테스트가 모두 통과한다.
- [ ] 오류 응답 snapshot 또는 필드 단위 검증이 OpenAPI·오류 계약과 일치한다.
- [ ] `./gradlew spotlessApply`와 `./gradlew check`가 통과한다.

### 7. Stateless Security와 현재 Guardian 복원 구현

JWT parsing을 직접 구현하지 않고 Resource Server가 access token을 검증하게 한다. 현재 Guardian 복원은 검증된 JWT subject 이후에
수행한다.

#### 작업

- [ ] stateless `SecurityFilterChain`을 구성하고 form login, HTTP Basic과 session 인증을 사용하지 않는다.
- [ ] OpenAPI에서 `security: []`인 M1 method·path만 공개하고 나머지는 기본적으로 인증을 요구한다.
- [ ] Resource Server의 `JwtDecoder`로 Bearer access token을 검증한다.
- [ ] 검증된 JWT subject를 UUID로 변환하고 현재 Guardian을 DB에서 조회하는 경계를 구현한다.
- [ ] Guardian이 없거나 `withdrawn`이면 `AUTH_TOKEN_INVALID`로 인증을 거부한다.
- [ ] `temporarily_restricted`는 인증 상태를 유지하고 M1 이후 제한 대상 기능에서 403으로 처리할 수 있게 상태를 보존한다.
- [ ] access token 만료와 그 밖의 누락·변조·오류를 계약의 서로 다른 code로 매핑한다.
- [ ] `AuthenticationEntryPoint`와 `AccessDeniedHandler`가 6단계의 공통 factory·writer를 사용하게 한다.
- [ ] 요청 ID filter가 Security 401·403보다 먼저 실행되는지 filter 순서를 검증한다.

#### 필수 테스트

- [ ] 공개 M1 endpoint에 access token 없이 접근 가능한 테스트
- [ ] 보호 endpoint의 token 누락·변조·만료·잘못된 subject 테스트
- [ ] 정상 JWT subject로 현재 Guardian과 status를 복원하는 테스트
- [ ] 존재하지 않는 Guardian과 `withdrawn` Guardian 거부 테스트
- [ ] Security 401과 403의 status·code·Content-Type·requestId 계약 테스트
- [ ] Security 오류와 MVC 오류가 같은 ProblemDetail 직렬화 규칙을 사용하는 테스트

#### 완료 조건

- [ ] 공개·보호 경로와 Security 오류 계약 테스트가 모두 통과한다.
- [ ] 직접 작성한 JWT parsing 또는 access token blacklist가 없다.
- [ ] 사람 개발자가 key 주입, filter 순서, 공개 경로와 Guardian 복원 방식을 검토했다.
- [ ] `./gradlew spotlessApply`와 `./gradlew check`가 통과한다.

### 8. M1 API를 endpoint별로 연결

아래 endpoint는 나열된 순서대로 하나씩 연결하고, 각 endpoint의 정상·실패 통합 테스트를 통과한 뒤 다음 endpoint로 이동한다.

#### 8.1 회원가입

- [ ] `POST /api/auth/signup` 요청을 validation하고 인증 service에 연결한다.
- [ ] 정상 응답의 status, token 필드와 정규화된 Guardian 응답을 계약과 일치시킨다.
- [ ] 잘못된 입력은 `VALIDATION_FAILED`, 정규화 email 중복은 `EMAIL_ALREADY_EXISTS`로 반환한다.
- [ ] 비밀번호 원문이 DB, 로그와 응답에 없음을 통합 테스트로 확인한다.

#### 8.2 로그인

- [ ] `POST /api/auth/login`을 인증 service에 연결한다.
- [ ] 성공 응답을 계약과 일치시키고 잘못된 email·비밀번호는 모두 `AUTH_INVALID_CREDENTIALS`로 반환한다.
- [ ] signup 전용 비밀번호 복잡도 규칙을 login에 다시 적용하지 않는다.

#### 8.3 Token 갱신

- [ ] `POST /api/auth/refresh`를 회전 transaction에 연결한다.
- [ ] 성공 응답에 새 token 쌍만 반환하고 이전 refresh token을 폐기한다.
- [ ] 만료·폐기·재사용·알 수 없는 token은 `AUTH_REFRESH_INVALID`로 반환한다.

#### 8.4 로그아웃

- [ ] `POST /api/auth/logout`을 멱등 폐기 동작에 연결한다.
- [ ] 최초·반복·알 수 없는 token 요청이 모두 계약의 성공 status를 반환한다.
- [ ] 응답 body를 반환하지 않는다.

#### 8.5 현재 Guardian 조회

- [ ] `GET /api/guardians/me`가 Security에서 복원한 현재 Guardian을 계약의 discriminator 형태로 반환한다.
- [ ] 개인 Guardian에는 gender가 있고 커플·가족 Guardian에는 gender가 없음을 확인한다.
- [ ] account status와 정규화된 email을 반환한다.

#### 8.6 현재 Guardian 수정

- [ ] `PATCH /api/guardians/me`가 profile type, gender와 identity visibility만 수정하게 한다.
- [ ] 개인·커플·가족의 조건부 validation을 다시 확인한다.
- [ ] 수정 대상이 현재 인증 Guardian으로 제한되는지 확인한다.
- [ ] 성공 응답을 수정된 Guardian discriminator 형태와 일치시킨다.

#### 필수 공통 테스트

- [ ] 각 endpoint의 OpenAPI 정상 status와 응답 필드 테스트
- [ ] signup, login, 현재 Guardian 응답이 정규화된 email만 반환하는 테스트
- [ ] 누락·빈 값·길이·enum·알 수 없는 JSON 필드 실패 테스트
- [ ] 각 endpoint에 정의된 400·401·409 응답 계약 테스트
- [ ] 모든 정상·오류 응답의 `X-Request-Id` 테스트
- [ ] 보호 endpoint의 token 누락·만료·변조 테스트

#### 완료 조건

- [ ] M1 여섯 endpoint의 정상·실패 통합 테스트가 모두 통과한다.
- [ ] OpenAPI에 없는 endpoint, 요청 필드 또는 응답 필드를 추가하지 않았다.
- [ ] controller에 transaction, token 생성 또는 repository 접근 로직이 없다.
- [ ] `./gradlew spotlessApply`와 `./gradlew check`가 통과한다.

### 9. 계약·보안 회귀 검증과 프론트 연결 준비

#### 작업

- [ ] OpenAPI 문서를 validator로 검사하고 M1 client type 생성 가능 여부를 확인한다.
- [ ] 생성된 Guardian `oneOf`와 discriminator, ProblemDetail 확장 필드를 확인한다.
- [ ] 프론트가 `application/problem+json`의 status, code, fieldErrors와 requestId를 파싱할 수 있는지 확인한다.
- [ ] 프론트가 동시 401에서 refresh 요청을 하나만 보내고 각 원 요청을 한 번만 재시도하는 정책을 확인한다.
- [ ] access token 만료 외의 401에서는 자동 refresh하지 않는지 확인한다.
- [ ] 전체 테스트에서 비밀번호, access token, refresh token과 내부 예외 정보가 출력되지 않는지 확인한다.
- [ ] application·테스트 로그와 실패 보고서에 원문 secret이 남지 않는지 확인한다.

#### 완료 조건

- [ ] 아래 `보안 검토 항목`과 `자동 검증`의 모든 항목을 완료했다.
- [ ] OpenAPI와 실제 M1 요청·응답 사이에 알려진 차이가 없다.
- [ ] `./gradlew spotlessApply`와 `./gradlew check`가 통과한다.

### 10. 수동 검증, 사람 검토와 M1 종료

#### 작업

- [ ] 로컬 PostgreSQL에서 signup → 보호 API → refresh → 이전 token 재사용 실패 → logout 흐름을 확인한다.
- [ ] 잘못된 signup, 중복 email, 잘못된 login, token 누락·만료·변조의 대표 실패 흐름을 확인한다.
- [ ] 정상·실패 응답의 Content-Type, body, `X-Request-Id`를 OpenAPI와 대조한다.
- [ ] 사람 개발자가 비밀번호, JWT key, token 원문, transaction, row lock, filter 순서와 DB migration을 최종 검토한다.
- [ ] `.github/pull_request_template.md`에 따라 Summary, Changes, Verification과 Checklist를 작성한다.
- [ ] Verification에 자동 테스트 명령과 수동 API 확인 결과를 기록한다.
- [ ] 보안·인증·DB·영속성·트랜잭션 변경과 AI가 만든 코드 범위를 Checklist에 기록한다.
- [ ] 현재 작업 트리에 M1과 무관한 변경이 섞이지 않았는지 확인한다.

#### 완료 조건

- [ ] 아래 `수동 검증과 검토` 및 `완료 게이트`를 모두 충족한다.
- [ ] M1을 완료로 바꾸기 직전에 `./gradlew check`를 다시 실행한다.
- [ ] M1을 `완료`, M2 하나만 `진행 중`으로 변경한다.

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
