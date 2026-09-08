# M1 TDD 테스트 케이스

이 문서는 M1 인증 기반을 TDD로 구현할 때 사용할 테스트 설계서다. 테스트의 실행 순서, 계층, 케이스 ID와 관찰 가능한 결과를 정의한다. 완료 여부는 [
`m1-auth-contract-checklist.md`](./m1-auth-contract-checklist.md)에서만 관리한다.

상세 계약값을 이 문서에 복사하지 않는다. HTTP 요청·응답과 token·email 정책은 [`openapi.yaml`](./openapi.yaml), 오류와 요청 ID는
[`error-response.md`](./error-response.md), 제품 인수 기준은 [`seed.yaml`](./seed.yaml)을 기준으로 한다. 문서가 충돌하면
테스트로 해석을 고정하지 말고 [`documentation-guide.md`](./documentation-guide.md#충돌-처리)에 따라 기준 문서부터 일치시킨다.

## TDD 진행 원칙

한 번에 아래 표의 한 행 또는 하나의 parameterized test 묶음만 진행한다.

1. **Red**: 아직 지원하지 않는 관찰 가능한 동작 하나를 테스트 이름으로 표현하고, 의도한 이유로 실패하는지 확인한다.
2. **Green**: 그 테스트를 통과시키는 최소 구현만 추가한다.
3. **Refactor**: 중복이나 이름을 정리하되 동작을 넓히지 않고 관련 테스트를 다시 실행한다.
4. **Regression**: 한 묶음이 끝나면 인접 묶음을, 한 단계가 끝나면 전체 `./gradlew check`를 실행한다.

컴파일 실패도 첫 Red로 사용할 수 있지만, 테스트가 실행되기 시작한 뒤에는 실패 메시지가 요구 동작을 설명해야 한다. mock 호출 횟수보다 반환값, 저장 상태, HTTP
응답처럼 외부에서 관찰 가능한 결과를 우선 검증한다.

## 테스트 계층과 경계

| 계층                   | 사용 대상                                                           | 검증할 것                                   | 피할 것                                        |
|------------------------|---------------------------------------------------------------------|---------------------------------------------|------------------------------------------------|
| 단위 테스트            | DTO validation, email 정규화, token 생성·hash, 오류 metadata        | 입력과 출력, 시간 경계, 민감정보 미노출     | Spring context와 DB를 불필요하게 시작하기      |
| 슬라이스 테스트        | MVC advice, JSON 직렬화, Security handler와 filter                  | HTTP status·header·content type·body 계약   | service 내부 구현을 재검증하기                 |
| PostgreSQL 통합 테스트 | migration, JPA mapping, unique/FK/check 제약, transaction, row lock | 실제 SQL 제약, commit·rollback, 동시성 결과 | H2로 PostgreSQL 동작을 대체하기                |
| API 통합 테스트        | M1의 여섯 endpoint와 filter chain                                   | 요청부터 DB까지의 정상·대표 실패 흐름       | 모든 DTO 경계를 endpoint마다 반복하기          |
| 수동 확인              | 실제 설정을 사용한 짧은 사용자 흐름                                 | 배포 형태의 연결과 응답 가독성              | 자동화할 수 있는 케이스를 수동 확인에만 남기기 |

시간 의존 테스트는 주입한 `Clock`을 사용하고 `sleep`을 사용하지 않는다. 동시성 테스트만 실제 PostgreSQL과 서로 다른 transaction을 사용한다. 각
테스트는 자기 데이터를 만들고, 실행 순서나 다른 테스트의 commit에 의존하지 않는다.

## 실행 순서

[`m1-auth-contract-checklist.md`](./m1-auth-contract-checklist.md#구현-순서)의 순서를 따른다. 현재 체크리스트에서 완료된 1~
4단계 테스트는 회귀 테스트로 유지하고, 5단계부터 같은 순서로 Red를 추가한다.

### 1. 입력 모델과 계약

| ID          | 계층                    | Given / When                                                                      | Then                                                                                                        |
|-------------|-------------------------|-----------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `M1-DTO-01` | 단위                    | 각 Guardian enum의 계약값을 JSON과 DB 값으로 변환한다.                            | 정확한 소문자 계약값으로 왕복하고 대소문자 변형·알 수 없는 값은 거부한다.                                   |
| `M1-DTO-02` | 단위·parameterized      | 개인·커플·가족의 유효한 signup/update 조합을 검증한다.                            | 개인은 gender가 있고 커플·가족은 gender가 없거나 `null`일 때만 성공한다.                                    |
| `M1-DTO-03` | 단위·parameterized      | signup 비밀번호의 최소·최대 길이와 각 허용 특수문자, 필수 문자군 경계를 검증한다. | 정확히 8~72자이고 허용 특수문자를 포함한 값만 성공하며, 공백·비허용 문자·누락 문자군·범위 밖 값은 실패한다. |
| `M1-DTO-04` | 단위·parameterized      | signup/login email에 앞뒤 공백과 대문자가 있다.                                   | 정규화 후 검증·저장에 사용할 canonical email이 만들어지고 `null`은 안전하게 처리된다.                       |
| `M1-DTO-05` | 단위·parameterized      | 정규화 전후의 email 길이·형식·문자 경계를 검증한다.                               | OpenAPI email 정책 밖의 값은 해당 필드 validation 오류가 된다.                                              |
| `M1-DTO-06` | 단위·parameterized      | login 비밀번호의 누락·1~72자 경계와 signup 복잡도 규칙 미적용을 검증한다.         | 1~72자는 signup 복잡도와 무관하게 통과하고, 누락·빈 값·72자 초과는 validation 오류가 된다.                  |
| `M1-DTO-07` | 단위·parameterized      | refresh token의 길이와 Base64 URL 문자 경계를 검증한다.                           | 계약 형식만 성공하며 누락·빈 값·길이 초과·비허용 문자는 실패한다.                                           |
| `M1-DTO-08` | JSON 단위·parameterized | 요청 JSON에 알 수 없는 필드나 enum 값이 들어온다.                                 | 역직렬화를 거부하며 이후 HTTP 계층에서 `VALIDATION_FAILED`로 변환할 수 있는 오류가 발생한다.                |

### 2. Migration과 JPA 영속성

| ID          | 계층                          | Given / When                                                                              | Then                                                                          |
|-------------|-------------------------------|-------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| `M1-DB-01`  | PostgreSQL 통합               | 빈 PostgreSQL에 Flyway를 실행한다.                                                        | V1부터 최신 migration이 순서대로 적용되고 Hibernate `validate`가 성공한다.    |
| `M1-DB-02`  | PostgreSQL 통합·parameterized | Guardian enum과 profile type·gender 조합을 직접 저장한다.                                 | 유효한 조합만 DB check 제약을 통과한다.                                       |
| `M1-DB-03`  | PostgreSQL 통합               | 대소문자만 다른 email을 두 transaction에서 저장한다.                                      | DB가 case-insensitive unique 제약으로 둘 중 하나를 거부한다.                  |
| `M1-DB-04`  | PostgreSQL 통합·parameterized | 잘못된 token hash, 중복 hash, 만료·폐기 시각 또는 없는 Guardian FK를 저장한다.            | hash 형식·고유성·수명 주기·FK 제약이 각각 잘못된 상태를 거부한다.             |
| `M1-DB-05`  | PostgreSQL 통합               | RefreshToken이 있는 Guardian을 삭제한다.                                                  | FK 정책에 따라 연결 token도 삭제된다.                                         |
| `M1-JPA-01` | PostgreSQL 통합               | Guardian을 저장하고 canonical email로 조회한다.                                           | UUID, 정규화 email과 enum이 동일하게 복원된다.                                |
| `M1-JPA-02` | PostgreSQL 통합               | RefreshToken을 저장하고 hash로 조회한다.                                                  | Guardian 단방향 연관관계와 생성·만료·폐기 시각이 복원된다.                    |
| `M1-JPA-03` | PostgreSQL 동시성             | transaction A가 token row를 pessimistic write lock으로 잡은 동안 B가 같은 row를 조회한다. | B는 A가 끝나기 전에 임계 구역을 통과하지 못하고 이후 최신 상태를 본다.        |
| `M1-JPA-04` | PostgreSQL 통합               | 대소문자를 무시하는 email unique 제약이 위반된다.                                         | 원인 예외에서 `uk_guardian_email_lower`를 식별해 email 충돌로 변환할 수 있다. |

### 3. 비밀번호와 Token 구성요소

| ID             | 계층               | Given / When                                                   | Then                                                                                                             |
|----------------|--------------------|----------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| `M1-CRYPTO-01` | 단위               | 같은 비밀번호를 두 번 BCrypt encode하고 match한다.             | 두 hash는 다르지만 원문은 모두 일치하며 잘못된 비밀번호는 불일치한다.                                            |
| `M1-CRYPTO-02` | 단위               | access token을 고정 Clock으로 발급한다.                        | `sub`, issuer, audience, algorithm, 발급·만료 시각과 TTL이 계약에 맞고 변경 가능한 Guardian 정보는 claim에 없다. |
| `M1-CRYPTO-03` | 단위·parameterized | 만료·변조·잘못된 issuer·audience·algorithm token을 decode한다. | 모두 검증 단계에서 거부된다.                                                                                     |
| `M1-CRYPTO-04` | 단위               | refresh token을 반복 생성하고 hash한다.                        | 형식·엔트로피 정책과 요청별 유일성을 만족하고 SHA-256 hash는 결정적이며 원문과 다르다.                           |
| `M1-CRYPTO-05` | context            | JWT secret이 누락되거나 최소 길이보다 짧다.                    | 애플리케이션 시작이 즉시 실패하고 secret은 오류에 노출되지 않는다.                                               |
| `M1-CRYPTO-06` | 계약 단위          | 애플리케이션 token 설정과 응답 상수를 OpenAPI 정책과 비교한다. | TTL, token type과 관련 상수가 기준 계약과 같다.                                                                  |

### 4. 인증 Application Service

권장 Red 순서는 signup 정상 → signup 충돌·rollback → login → refresh 정상 → refresh 실패 → refresh 동시성·rollback →
logout이다.

| ID           | 계층                          | Given / When                                                                       | Then                                                                                                                                |
|--------------|-------------------------------|------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `M1-AUTH-01` | PostgreSQL 통합               | 유효한 signup 요청을 처리한다.                                                     | canonical email과 BCrypt hash를 가진 active Guardian, hash만 저장된 refresh token, 계약에 맞는 token 쌍과 Guardian 응답이 생성된다. |
| `M1-AUTH-02` | PostgreSQL 통합               | 정규화하면 기존 email과 같은 signup을 요청한다.                                    | email 충돌 결과가 나고 Guardian·refresh token이 추가되지 않는다.                                                                    |
| `M1-AUTH-03` | PostgreSQL 통합               | signup 중 token 발급 또는 refresh token 저장이 실패한다.                           | Guardian 저장과 refresh token 저장이 한 transaction으로 rollback된다.                                                               |
| `M1-AUTH-04` | PostgreSQL 통합               | 대소문자와 앞뒤 공백이 다른 email, 올바른 비밀번호로 login한다.                    | 같은 Guardian을 조회해 새 token 쌍을 발급한다.                                                                                      |
| `M1-AUTH-05` | 단위·parameterized            | 존재하지 않는 email 또는 잘못된 비밀번호로 login한다.                              | 둘 다 구분되지 않는 `AUTH_INVALID_CREDENTIALS` 결과이며 token을 만들지 않는다.                                                      |
| `M1-AUTH-06` | PostgreSQL 통합·parameterized | withdrawn 또는 temporarily restricted Guardian이 login한다.                        | withdrawn은 자격 증명 오류로 거부하고 temporarily restricted는 인증에 성공한다.                                                     |
| `M1-AUTH-07` | PostgreSQL 통합               | 유효한 refresh token을 사용한다.                                                   | 기존 row가 폐기되고 새 hash row와 새 token 쌍이 같은 transaction에서 생성되며 원문은 저장되지 않는다.                               |
| `M1-AUTH-08` | PostgreSQL 통합·parameterized | 만료·폐기·회전된 token 또는 저장되지 않은 token으로 refresh한다.                   | 모두 동일한 `AUTH_REFRESH_INVALID` 결과이며 새 token을 만들지 않는다.                                                               |
| `M1-AUTH-09` | PostgreSQL 동시성             | 같은 refresh token으로 두 transaction을 동시에 시작한다.                           | 정확히 하나만 회전에 성공하고 다른 하나는 `AUTH_REFRESH_INVALID`가 되며 활성 후속 token은 하나다.                                   |
| `M1-AUTH-10` | PostgreSQL 통합               | 기존 token 폐기 후 새 token 저장이 실패한다.                                       | 폐기와 신규 저장이 함께 rollback되어 기존 token 상태가 요청 전과 같다.                                                              |
| `M1-AUTH-11` | PostgreSQL 통합·parameterized | 유효·이미 폐기·저장되지 않은 올바른 형식의 token으로 logout한다.                   | 모두 성공하며 존재한 token만 폐기되고 추가 token은 생성되지 않는다.                                                                 |
| `M1-AUTH-12` | PostgreSQL 동시성             | 동시에 같은 normalized email의 signup 두 요청이 중복 검사 후 함께 저장을 시도한다. | 정확히 하나만 성공하고, 다른 하나는 EmailAlreadyExistsException이 된다. Guardian과 RefreshToken은 성공한 요청의 것만 남는다.        |

### 5. 요청 ID와 공통 REST 오류

요청 ID filter를 먼저 완성한 뒤 ProblemDetail factory·writer, MVC advice 순으로 진행한다.

| ID          | 계층                       | Given / When                                                        | Then                                                                                       |
|-------------|----------------------------|---------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| `M1-REQ-01` | filter·API                 | 정상 요청과 MVC 오류 요청을 각각 여러 번 보낸다.                    | 매 응답에 계약 형식의 서로 다른 서버 생성 `X-Request-Id`가 있다.                           |
| `M1-REQ-02` | filter                     | 클라이언트가 `X-Request-Id`를 보낸다.                               | 서버는 입력값을 무시하고 새 값을 attribute, MDC와 응답 header에 사용한다.                  |
| `M1-REQ-03` | filter·오류                | 요청 처리 중 ProblemDetail을 만든다.                                | request attribute, MDC, header와 body의 `requestId`가 같다.                                |
| `M1-REQ-04` | filter 단위                | 기존 MDC 값이 없는 요청과 있는 요청을 각각 정상·예외 종료한다.      | `finally`에서 이전 값을 복원하거나 새 값을 제거해 다음 요청으로 유출하지 않는다.           |
| `M1-ERR-01` | MVC 슬라이스·parameterized | Bean Validation, 잘못된 JSON·enum, query·path type 오류가 발생한다. | HTTP/body status, `VALIDATION_FAILED`, `fieldErrors` 유무와 instance가 오류 계약에 맞는다. |
| `M1-ERR-02` | MVC 슬라이스·parameterized | email 중복, 리소스 없음 또는 상태 충돌 예외가 발생한다.             | 각 status·type·title·code와 `fieldErrors` 규칙이 기준 오류 metadata와 같다.                |
| `M1-ERR-03` | MVC 슬라이스               | 예상하지 못한 예외가 발생한다.                                      | 500 공통 오류만 반환하고 exception class, stack trace, SQL과 민감정보는 body에 없다.       |
| `M1-ERR-04` | MVC 슬라이스·parameterized | 지원하지 않는 HTTP method 또는 Accept로 요청한다.                   | 405/406의 `application/problem+json` 응답과 안정적인 code를 반환한다.                      |
| `M1-ERR-05` | 공통 assertion             | 위의 모든 오류 응답을 검증한다.                                     | Content-Type, HTTP/body status, 필수 ProblemDetail 필드와 요청 ID가 항상 일치한다.         |

### 6. Stateless Security와 현재 Guardian 복원

| ID          | 계층                        | Given / When                                                                                 | Then                                                                                                                                    |
|-------------|-----------------------------|----------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `M1-SEC-01` | Security 통합·parameterized | M1의 네 auth endpoint에 access token 없이 접근한다.                                          | Security는 요청을 공개 경로로 통과시키며 MVC validation은 그대로 적용된다.                                                              |
| `M1-SEC-02` | Security 통합               | auth endpoint 외의 경로에 token 없이 접근한다.                                               | 기본 정책이 인증을 요구하며 `AUTH_TOKEN_INVALID` 401 ProblemDetail을 반환한다.                                                          |
| `M1-SEC-03` | Security 통합·parameterized | 보호 경로에 누락·변조·잘못된 issuer/audience/subject token을 보낸다.                         | 모두 `AUTH_TOKEN_INVALID`이고 Guardian 인증 객체를 만들지 않는다.                                                                       |
| `M1-SEC-04` | Security 통합               | 만료된 access token을 보호 경로에 보낸다.                                                    | `AUTH_TOKEN_EXPIRED`로 구분한 401을 반환한다.                                                                                           |
| `M1-SEC-05` | Security 통합               | 유효한 JWT subject가 active 또는 temporarily restricted Guardian을 가리킨다.                 | 매 요청 DB에서 현재 Guardian과 status를 복원하고 인증 상태를 유지한다.                                                                  |
| `M1-SEC-06` | Security 통합·parameterized | JWT subject의 Guardian이 없거나 withdrawn 상태다.                                            | `AUTH_TOKEN_INVALID`로 인증을 거부한다.                                                                                                 |
| `M1-SEC-07` | handler·Security 슬라이스   | 인증된 사용자의 접근이 거부된다.                                                             | 공통 writer를 사용한 `FORBIDDEN` 403 ProblemDetail을 반환한다. 테스트를 위해 production endpoint를 추가하지 않는다.                     |
| `M1-SEC-08` | Security 통합               | 401 또는 403이 filter chain에서 발생한다.                                                    | 요청 ID가 header와 body에 존재해 요청 ID filter가 Security보다 먼저 실행됐음을 보인다.                                                  |
| `M1-SEC-09` | context·Security 통합       | 같은 access token으로 session cookie 없이 요청을 연속해서 처리하고 Security 설정을 검사한다. | 매 요청 독립적으로 인증하며 session, form login과 HTTP Basic을 사용하지 않는다. access-token blacklist가 없는지는 코드 검토로 확인한다. |

### 7. M1 API

각 endpoint에서 정상 흐름 하나를 먼저 end-to-end로 통과시킨 뒤 대표 실패를 추가한다. DTO 단위 테스트의 모든 경계 조합을 HTTP 테스트에서 반복하지 않고,
역직렬화·validation·오류 변환이 연결되는 대표값만 선택한다.

| ID                 | Endpoint                  | Given / When                                                                  | Then                                                                                                             |
|--------------------|---------------------------|-------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| `M1-API-SU-01`     | `POST /api/auth/signup`   | 개인·커플·가족의 유효한 요청을 보낸다.                                        | 201과 계약 형태의 AuthResponse를 반환하고 canonical email, discriminator별 gender와 active 상태를 저장·응답한다. |
| `M1-API-SU-02`     | signup                    | 누락·잘못된 조합·알 수 없는 필드의 대표 요청을 보낸다.                        | 400 `VALIDATION_FAILED`이며 비밀번호가 응답과 로그에 없다.                                                       |
| `M1-API-SU-03`     | signup                    | 정규화하면 기존 email과 같은 요청을 보낸다.                                   | 409 `EMAIL_ALREADY_EXISTS`, email `fieldErrors`를 반환하고 데이터가 추가되지 않는다.                             |
| `M1-API-LI-01`     | `POST /api/auth/login`    | canonical 변형 email과 올바른 비밀번호를 보낸다.                              | 200 AuthResponse와 해당 Guardian을 반환하고 새 refresh token hash를 저장한다.                                    |
| `M1-API-LI-02`     | login                     | 잘못된 email과 잘못된 비밀번호를 각각 보낸다.                                 | 두 응답 모두 동일한 401 `AUTH_INVALID_CREDENTIALS` 계약으로 계정 존재 여부를 숨긴다.                             |
| `M1-API-LI-03`     | login                     | signup 복잡도에 맞지 않지만 길이는 유효한 기존 비밀번호를 보낸다.             | 400으로 조기 거부하지 않고 인증 service까지 전달한다.                                                            |
| `M1-API-RF-01`     | `POST /api/auth/refresh`  | 유효한 token으로 요청한 뒤 이전 token을 다시 사용한다.                        | 첫 요청은 200 새 TokenResponse, 두 번째는 401 `AUTH_REFRESH_INVALID`다.                                          |
| `M1-API-RF-02`     | refresh                   | 형식이 잘못된 token과 형식은 맞지만 만료·폐기·알 수 없는 token을 보낸다.      | 형식 오류는 400 validation, 수명 주기 오류는 같은 401 `AUTH_REFRESH_INVALID`로 구분된다.                         |
| `M1-API-LO-01`     | `POST /api/auth/logout`   | 같은 유효 token으로 두 번 요청하고 알 수 없는 올바른 형식의 token도 요청한다. | 모두 body 없는 204이며 이후 기존 token refresh는 실패한다.                                                       |
| `M1-API-ME-01`     | `GET /api/guardians/me`   | 각 profile type의 유효한 access token으로 조회한다.                           | 200 Guardian discriminator 응답이며 개인만 gender가 있고 canonical email과 현재 status가 있다.                   |
| `M1-API-ME-02`     | GET me                    | token이 누락·만료·변조되었다.                                                 | 각각 Security 401 계약을 따르고 controller는 실행되지 않는다.                                                    |
| `M1-API-ME-03`     | `PATCH /api/guardians/me` | profile type, gender와 identity visibility를 유효한 조합으로 변경한다.        | 현재 인증 Guardian만 수정하고 200의 discriminator 응답과 DB 상태가 같다.                                         |
| `M1-API-ME-04`     | PATCH me                  | 개인·커플·가족 조건을 위반하거나 허용되지 않은 필드를 보낸다.                 | 400 `VALIDATION_FAILED`이고 기존 Guardian 상태는 바뀌지 않는다.                                                  |
| `M1-API-ME-05`     | PATCH me                  | 다른 Guardian도 존재하지만 현재 token으로 수정한다.                           | token subject의 Guardian만 변경되어 수평 권한 경계가 유지된다.                                                   |
| `M1-API-COMMON-01` | 여섯 endpoint             | 정상·오류 대표 요청을 보낸다.                                                 | OpenAPI status와 필드 집합을 지키고 모든 응답에 새 `X-Request-Id`가 있다.                                        |
| `M1-API-COMMON-02` | 여섯 endpoint             | 지원하지 않는 method/Accept 또는 의도적으로 만든 내부 실패를 보낸다.          | 405·406·500 공통 오류 계약을 지키고 내부 정보와 secret을 노출하지 않는다.                                        |

### 8. 계약·보안 회귀와 수동 흐름

| ID          | 종류 | 시나리오                                                                         | 통과 조건                                                                                                           |
|-------------|------|----------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `M1-REG-01` | 자동 | OpenAPI validator와 client type 생성을 실행한다.                                 | M1 요청·응답, Guardian `oneOf` discriminator와 ProblemDetail 확장 필드를 생성할 수 있다.                            |
| `M1-REG-02` | 자동 | 전체 테스트 로그와 실패 보고서를 검색한다.                                       | 비밀번호, access/refresh token 원문, 내부 예외와 SQL이 남지 않는다. 테스트 fixture의 고정 secret도 출력하지 않는다. |
| `M1-REG-03` | 자동 | 전체 `./gradlew check`를 실행한다.                                               | 단위·PostgreSQL 통합·API·형식 검사가 모두 통과한다.                                                                 |
| `M1-MAN-01` | 수동 | signup → GET/PATCH me → refresh → 이전 token 재사용 → logout을 실행한다.         | 각 성공·실패 status, body, Content-Type과 요청 ID가 기준 계약과 같다.                                               |
| `M1-MAN-02` | 수동 | 잘못된 signup, 중복 email, 잘못된 login, 누락·만료·변조 access token을 확인한다. | 앱이 분기할 status와 code가 안정적이고 민감정보가 응답·로그에 없다.                                                 |

프론트의 동시 401 단일 refresh와 요청별 1회 재시도는 OpenAPI token 정책의 소비자 검증 항목이다. 백엔드 M1 테스트에서는 refresh rotation과
동시 사용 시 하나만 성공함을 검증하고, 프론트 동작 자체를 백엔드 테스트로 모사하지 않는다.

## 공통 Assertion 체크

API 오류 테스트는 가능한 경우 하나의 test helper로 다음 불변식을 검증하되, helper가 실제 기대값을 숨기지 않게 한다.

- HTTP status와 body `status`가 같다.
- `Content-Type`은 `application/problem+json`이다.
- `type`, `title`, `detail`, `instance`, `code`, `requestId`가 존재하고 해당 오류 계약과 맞는다.
- validation 오류에만 필요한 `fieldErrors`가 있으며 민감한 입력값은 포함하지 않는다.
- `X-Request-Id`와 body `requestId`가 같고 서버 생성 형식을 만족한다.
- 성공 응답은 OpenAPI의 필수 필드만 포함하고 비밀번호·hash·내부 수명 주기 상태를 노출하지 않는다.

## 완료 판정

이 문서의 케이스가 구현됐다는 사실만으로 M1을 완료 처리하지 않는다. 자동 테스트 후 수동 흐름, OpenAPI 대조, 보안·transaction·row lock에 대한 사람
검토를 마치고 [`m1-auth-contract-checklist.md`](./m1-auth-contract-checklist.md#완료-게이트)의 모든 항목을 충족해야 한다.
