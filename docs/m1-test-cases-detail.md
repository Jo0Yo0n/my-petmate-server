# M1 테스트 케이스 상세

[테스트 목록](./m1-test-cases.md)에서 ID를 선택해 이 문서로 이동한다. 각 테스트는 대응 메서드와 검증할 결과만 기술한다.
클래스·메서드 이름은 현재 코드와의 대응이며 통과·완료 상태를 뜻하지 않는다. 미구현 항목의 클래스·메서드 이름은 권장 이름이고 같은 의미의 이름을 사용해도 된다.

필수 목록에서 제외된 기존 테스트는 회귀 테스트로 유지한다. 제외된 ID 때문에 새 테스트를 작성하거나 이전 단계의 완료를 보류하지 않는다.

## 기준과 완료 범위

- 입력·응답·token 정책은 [OpenAPI](./openapi.yaml), 오류·요청 ID는 [오류 계약](./error-response.md)이 기준이다. 상세 계약을 이
  문서에서 새로 정의하지 않는다.
- 같은 분기는 대표값, 길이·시간·조건부 입력은 동작이 달라지는 경계만 검증한다. HTTP에서 DTO·service·JWT decoder의 상세 조합을 반복하지 않는다.
- 시간은 고정 가능한 Clock으로 검증하고 DB·transaction·row lock은 실제 PostgreSQL로 검증한다. 이전 테스트의 실행이나 데이터에 의존하지 않는다.
- 관련 테스트를 실행하고 핵심 기대 결과가 충족되면 해당 ID를 완료한다. 단계 종료 시 전체 회귀와 형식 검사를 수행한다.
- 각 단계의 완료에는 현재 단계와 이전 단계만 필요하다. 이후 연결 검증은 그 연결을 구현하는 단계의 별도 ID로 관리한다.

## 공통 Assertion 체크

공통 항목은 개별 HTTP 테스트에 포함하며 별도 요청이나 테스트 메서드를 추가하지 않는다.

### 오류 응답

- HTTP status와 body status가 같고 Content-Type은 application/problem+json이다.
- type·title·code는 기대 오류의 계약값이며 detail은 비어 있지 않다. instance는 요청 경로이고 민감한 query 값을 포함하지 않는다.
- validation fieldErrors의 field·비어 있지 않은 reason을 확인한다. 다른 오류에서는 fieldErrors를 생략한다.
- 서버 생성 요청 ID 형식을 만족하고 header와 body의 requestId가 같다.
- detail과 reason의 문장 전체는 계약이 문구를 고정한 경우에만 비교한다.

### 정보 노출 검증 범위

- 내부 예외·stack trace·SQL·예외에 넣은 민감정보의 응답 노출은 M1-ERR-03B에서 집중 검증한다. 다른 오류 테스트마다 같은 부재 검사를 반복하지 않는다.
- 비밀번호·hash·token 원문 등 민감정보의 응답 노출은 해당 값을 취급하는 대표 응답에서 확인한다. 동일 응답 구조를 사용하는 입력 조합마다 반복하지 않는다.
- 로그·실패 보고서의 민감정보 노출은 M1-REG-02에서 한 번 확인한다.

### 성공 응답

- 해당 API의 status·허용 필드·필수 필드를 지키고 서버 생성 요청 ID header가 있다.
- 비밀번호·hash·내부 수명 주기 상태의 노출 검사는 위 대표 응답 범위에 따른다.

### writer의 범위

writer는 전달받은 ProblemDetail의 직렬화를 검증한다. 요청 ID 생성·header 설정 및 filter 실행 순서는 writer의 책임이 아니다.

## 1. 입력 모델과 계약

이 단계는 DTO·enum·JSON 입력 자체를 검증한다. HTTP endpoint는 필요하지 않다.

### M1-DTO-01A

| 항목   | 내용                                                |
|--------|-----------------------------------------------------|
| 클래스 | `GuardianEnumTest`                                  |
| 메서드 | `serializesEnumsAsContractValues()`                 |
| 검증   | Guardian enum을 계약의 소문자 JSON 값으로 출력한다. |

### M1-DTO-01B

| 항목   | 내용                                              |
|--------|---------------------------------------------------|
| 클래스 | `GuardianEnumTest`                                |
| 메서드 | `deserializesExactContractValues()`               |
| 검증   | 계약의 JSON 값을 정확한 Guardian enum으로 읽는다. |

### M1-DTO-01C

| 항목   | 내용                                           |
|--------|------------------------------------------------|
| 클래스 | `GuardianEnumTest`                             |
| 메서드 | `rejectsCaseVariantsAndUnknownValues()`        |
| 검증   | 대소문자 변형과 알 수 없는 enum 값을 거부한다. |

### M1-DTO-01D

| 항목   | 내용                                     |
|--------|------------------------------------------|
| 클래스 | `GuardianEnumTest`                       |
| 메서드 | `convertersUseLowercaseDatabaseValues()` |
| 검증   | enum과 계약의 DB 값이 왕복 변환된다.     |

### M1-DTO-02A

| 항목   | 내용                                                            |
|--------|-----------------------------------------------------------------|
| 클래스 | `RequestValidationTest`                                         |
| 메서드 | `acceptsValidIndividualCoupleAndFamilySignupRequests()`         |
| 검증   | 개인·커플·가족의 유효한 유형·성별 조합은 validation에 성공한다. |

### M1-DTO-02B

| 항목   | 내용                                                       |
|--------|------------------------------------------------------------|
| 클래스 | `RequestValidationTest`                                    |
| 메서드 | `requiresGenderForIndividualSignup()`                      |
| 검증   | 개인 유형에서 성별이 없으면 gender validation 오류가 된다. |

### M1-DTO-02C

| 항목   | 내용                                                            |
|--------|-----------------------------------------------------------------|
| 클래스 | `RequestValidationTest`                                         |
| 메서드 | `forbidsGenderForCoupleAndFamilySignup()`                       |
| 검증   | 커플·가족 유형에 성별을 지정하면 gender validation 오류가 된다. |

### M1-DTO-02D

| 항목   | 내용                                                                            |
|--------|---------------------------------------------------------------------------------|
| 클래스 | `RequestValidationTest`                                                         |
| 메서드 | `appliesTheSameProfileRuleToGuardianUpdates()`                                  |
| 검증   | 수정 DTO에도 유형·성별 조건이 적용된다. HTTP endpoint 없이 DTO 자체를 검증한다. |

### M1-DTO-03A

| 항목   | 내용                                                           |
|--------|----------------------------------------------------------------|
| 클래스 | `RequestValidationTest`                                        |
| 메서드 | `acceptsPasswordContainingEveryRequiredAsciiCharacterGroup()`  |
| 검증   | 계약의 길이와 필수 문자군을 만족한 signup 비밀번호를 허용한다. |

### M1-DTO-03B

| 항목   | 내용                                                                |
|--------|---------------------------------------------------------------------|
| 클래스 | `RequestValidationTest`                                             |
| 메서드 | `rejectsPasswordMissingAnyRequiredCharacterGroup()`                 |
| 검증   | 필수 문자군이 빠진 대표 입력을 password validation 오류로 거부한다. |

### M1-DTO-03C

| 항목   | 내용                                                      |
|--------|-----------------------------------------------------------|
| 클래스 | `RequestValidationTest`                                   |
| 메서드 | `rejectsNonAsciiWhitespaceAndOutOfRangePasswords()`       |
| 검증   | 길이 범위 밖, 공백 및 비허용 문자의 대표 입력을 거부한다. |

### M1-DTO-03D

| 항목   | 내용                                                      |
|--------|-----------------------------------------------------------|
| 클래스 | `RequestValidationTest`                                   |
| 메서드 | `rejectsBlankSignupPassword()`                            |
| 검증   | 빈 signup 비밀번호를 password validation 오류로 거부한다. |

### M1-DTO-04

| 항목   | 내용                                                                                                        |
|--------|-------------------------------------------------------------------------------------------------------------|
| 클래스 | `RequestValidationTest`                                                                                     |
| 메서드 | `normalizesSignupAndLoginEmailsBeforeValidation()`                                                          |
| 검증   | signup/login email의 앞뒤 공백과 대문자를 같은 canonical 값으로 정규화하고 정규화 후 validation에 성공한다. |

### M1-DTO-05A

| 항목   | 내용                                                 |
|--------|------------------------------------------------------|
| 클래스 | `RequestValidationTest`                              |
| 메서드 | `validatesNormalizedEmailBlankAndLengthBoundaries()` |
| 검증 1 | 정규화 후 빈 email과 길이 범위 밖 입력을 거부한다.   |
| 검증 2 | 허용 경계 입력은 통과한다.                           |

### M1-DTO-05B

| 항목   | 내용                                                         |
|--------|--------------------------------------------------------------|
| 클래스 | `RequestValidationTest`                                      |
| 메서드 | `rejectsNonAsciiSignupAndLoginEmails()`                      |
| 검증   | signup/login에서 비허용 문자를 포함한 대표 email을 거부한다. |

### M1-DTO-05C

| 항목   | 내용                                                                                |
|--------|-------------------------------------------------------------------------------------|
| 클래스 | `RequestValidationTest`                                                             |
| 메서드 | `validatesEmailAndRequiredSignupFields()`                                           |
| 검증   | 필수 signup 입력의 누락과 잘못된 email 형식을 해당 필드 validation 오류로 거부한다. |

### M1-DTO-06A

| 항목   | 내용                                                                                               |
|--------|----------------------------------------------------------------------------------------------------|
| 클래스 | `RequestValidationTest`                                                                            |
| 메서드 | `loginDoesNotReapplySignupPasswordComplexity()`                                                    |
| 검증   | 최소·최대 허용 길이의 단순 비밀번호가 signup 복잡도에 맞지 않아도 login DTO validation을 통과한다. |

### M1-DTO-06B

| 항목   | 내용                            |
|--------|---------------------------------|
| 클래스 | `RequestValidationTest`         |
| 메서드 | `rejectsMissingLoginPassword()` |
| 검증   | null login 비밀번호를 거부한다. |

### M1-DTO-06C

| 항목   | 내용                          |
|--------|-------------------------------|
| 클래스 | `RequestValidationTest`       |
| 메서드 | `rejectsEmptyLoginPassword()` |
| 검증   | 빈 login 비밀번호를 거부한다. |

### M1-DTO-06D

| 항목   | 내용                                                 |
|--------|------------------------------------------------------|
| 클래스 | `RequestValidationTest`                              |
| 메서드 | `rejectsLoginPasswordLongerThan72Characters()`       |
| 검증   | 계약의 최대 길이를 초과한 login 비밀번호를 거부한다. |

### M1-DTO-07A

| 항목   | 내용                                                             |
|--------|------------------------------------------------------------------|
| 클래스 | `RequestValidationTest`                                          |
| 메서드 | `acceptsRefreshTokenWithBase64UrlCharacters()`                   |
| 검증   | 계약의 길이와 Base64 URL 문자로 구성된 refresh token을 허용한다. |

### M1-DTO-07B

| 항목   | 내용                                           |
|--------|------------------------------------------------|
| 클래스 | `RequestValidationTest`                        |
| 메서드 | `rejectsRefreshTokenShorterThan43Characters()` |
| 검증   | 계약 길이보다 짧은 refresh token을 거부한다.   |

### M1-DTO-07C

| 항목   | 내용                                          |
|--------|-----------------------------------------------|
| 클래스 | `RequestValidationTest`                       |
| 메서드 | `rejectsRefreshTokenLongerThan43Characters()` |
| 검증   | 계약 길이보다 긴 refresh token을 거부한다.    |

### M1-DTO-07D

| 항목   | 내용                                                                |
|--------|---------------------------------------------------------------------|
| 클래스 | `RequestValidationTest`                                             |
| 메서드 | `rejectsRefreshTokenWithNonBase64UrlCharacter()`                    |
| 검증   | Base64 URL 형식에 없는 대표 문자를 포함한 refresh token을 거부한다. |

### M1-DTO-07E

| 항목   | 내용                           |
|--------|--------------------------------|
| 클래스 | `RequestValidationTest`        |
| 메서드 | `rejectsMissingRefreshToken()` |
| 검증   | null refresh token을 거부한다. |

### M1-DTO-07F

| 항목   | 내용                         |
|--------|------------------------------|
| 클래스 | `RequestValidationTest`      |
| 메서드 | `rejectsEmptyRefreshToken()` |
| 검증   | 빈 refresh token을 거부한다. |

### M1-DTO-08A

| 항목   | 내용                                                        |
|--------|-------------------------------------------------------------|
| 클래스 | `SignupRequestJsonTest`                                     |
| 메서드 | `rejectsUnknownSignupJsonField()`                           |
| 검증   | signup JSON의 알 수 없는 필드를 역직렬화 단계에서 거부한다. |

### M1-DTO-08B

| 항목   | 내용                                                       |
|--------|------------------------------------------------------------|
| 클래스 | `LoginRequestJsonTest`                                     |
| 메서드 | `rejectsUnknownLoginJsonField()`                           |
| 검증   | login JSON의 알 수 없는 필드를 역직렬화 단계에서 거부한다. |

### M1-DTO-08C

| 항목   | 내용                                                         |
|--------|--------------------------------------------------------------|
| 클래스 | `RefreshRequestJsonTest`                                     |
| 메서드 | `rejectsUnknownRefreshJsonField()`                           |
| 검증   | refresh JSON의 알 수 없는 필드를 역직렬화 단계에서 거부한다. |

### M1-DTO-08D

| 항목   | 내용                                                 |
|--------|------------------------------------------------------|
| 클래스 | `SignupRequestJsonTest`                              |
| 메서드 | `rejectsUnknownProfileTypeInSignupJson()`            |
| 검증   | 알 수 없는 profileType을 역직렬화 단계에서 거부한다. |

## 2. Migration과 JPA 영속성

migration 검증은 DB schema에서, JPA 검증은 entity·repository에서 완료한다. AuthService를 사용하지 않는다.

### M1-DB-01

| 항목   | 내용                                                             |
|--------|------------------------------------------------------------------|
| 클래스 | `DatabaseMigrationIntegrationTest`                               |
| 메서드 | `appliesAllMigrationsToAnEmptyPostgresDatabase()`                |
| 검증 1 | 빈 PostgreSQL에 최신 migration까지 순서대로 적용된다.            |
| 검증 2 | 이 항목의 완료에 JPA entity나 이후 service 구현은 필요하지 않다. |

### M1-DB-02A

| 항목   | 내용                                                         |
|--------|--------------------------------------------------------------|
| 클래스 | `DatabaseMigrationIntegrationTest`                           |
| 메서드 | `guardianAcceptsValidEnumAndProfileTypeGenderCombinations()` |
| 검증   | 유효한 enum·유형·성별 조합을 DB에 저장할 수 있다.            |

### M1-DB-02B

| 항목   | 내용                                                           |
|--------|----------------------------------------------------------------|
| 클래스 | `DatabaseMigrationIntegrationTest`                             |
| 메서드 | `guardianRejectsInvalidEnumAndProfileTypeGenderCombinations()` |
| 검증   | 잘못된 enum·유형·성별 조합을 DB check 제약이 거부한다.         |

### M1-DB-03

| 항목   | 내용                                                           |
|--------|----------------------------------------------------------------|
| 클래스 | `DatabaseMigrationIntegrationTest`                             |
| 메서드 | `guardianRejectsEmailsThatDifferOnlyByCase()`                  |
| 검증   | 대소문자만 다른 email의 중복 저장을 DB unique 제약이 거부한다. |

### M1-DB-04

| 항목   | 내용                                                                                      |
|--------|-------------------------------------------------------------------------------------------|
| 클래스 | `DatabaseMigrationIntegrationTest`                                                        |
| 메서드 | `refreshTokenRejectsInvalidHashTimesAndGuardian()`                                        |
| 검증   | hash 형식·중복, 잘못된 생성·만료·폐기 시각 및 없는 Guardian FK를 각각 DB 제약이 거부한다. |

### M1-DB-05

| 항목   | 내용                                                                |
|--------|---------------------------------------------------------------------|
| 클래스 | `DatabaseMigrationIntegrationTest`                                  |
| 메서드 | `deletingGuardianDeletesRefreshTokens()`                            |
| 검증   | Guardian을 삭제하면 FK 정책에 따라 연결된 refresh token도 삭제된다. |

### M1-JPA-01

| 항목   | 내용                                                                     |
|--------|--------------------------------------------------------------------------|
| 클래스 | `GuardianPersistenceIntegrationTest`                                     |
| 메서드 | `savesGuardianAndRestoresNormalizedEmailAndEnums()`                      |
| 검증 1 | 저장한 Guardian의 UUID·canonical email·enum이 조회 시 동일하게 복원된다. |
| 검증 2 | 현재 entity mapping과 DB schema가 일치한다.                              |

### M1-JPA-02

| 항목   | 내용                                                                         |
|--------|------------------------------------------------------------------------------|
| 클래스 | `GuardianPersistenceIntegrationTest`                                         |
| 메서드 | `savesRefreshTokenAndFindsItByHashWithGuardianAssociation()`                 |
| 검증   | hash로 조회한 RefreshToken의 Guardian 관계와 생성·만료·폐기 시각이 복원된다. |

### M1-JPA-03

| 항목   | 내용                                                                                                   |
|--------|--------------------------------------------------------------------------------------------------------|
| 클래스 | `GuardianPersistenceIntegrationTest`                                                                   |
| 메서드 | `pessimisticWriteLockSerializesConcurrentAccessToTheSameToken()`                                       |
| 검증 1 | 같은 row의 lock을 얻으려는 두 transaction 중 두 번째는 첫 번째 transaction 종료 전 lock을 얻지 못한다. |
| 검증 2 | repository 단계에서 완료하며 AuthService의 token 회전은 사용하지 않는다.                               |

## 3. 비밀번호와 Token 구성요소

token·설정 자체로 완료하며 auth service·endpoint는 필요하지 않다. BCrypt 시간 측정은 체크리스트의 측정 항목으로 별도 관리한다.

### M1-CRYPTO-01A

| 항목   | 내용                                           |
|--------|------------------------------------------------|
| 클래스 | `PasswordConfigTest`                           |
| 메서드 | `encodesPassword()`                            |
| 검증   | 원문 비밀번호가 생성한 BCrypt hash와 일치한다. |

### M1-CRYPTO-02A

| 항목   | 내용                                                                                              |
|--------|---------------------------------------------------------------------------------------------------|
| 클래스 | `AccessTokenIssuerTest`                                                                           |
| 메서드 | `issuesAnAccessTokenWithContractClaims()`                                                         |
| 검증 1 | 고정 시각으로 발급한 JWT의 subject·issuer·audience·algorithm·발급 및 만료 시각이 계약과 일치한다. |
| 검증 2 | 변경 가능한 Guardian 정보나 민감정보는 claim에 없다.                                              |

### M1-CRYPTO-02B

| 항목   | 내용                                                                   |
|--------|------------------------------------------------------------------------|
| 클래스 | `JwtConfigTest`                                                        |
| 메서드 | `encodesAndDecodesAnHs256JwtWithTheConfiguredIssuerAndAudience()`      |
| 검증   | 현재 설정의 encoder가 발급한 JWT를 같은 설정의 decoder로 읽을 수 있다. |

### M1-CRYPTO-03A

| 항목   | 내용                                    |
|--------|-----------------------------------------|
| 클래스 | `JwtConfigTest`                         |
| 메서드 | `rejectsJwtWithAnUnexpectedIssuer()`    |
| 검증   | 다른 issuer의 JWT를 decoder가 거부한다. |

### M1-CRYPTO-03B

| 항목   | 내용                                      |
|--------|-------------------------------------------|
| 클래스 | `JwtConfigTest`                           |
| 메서드 | `rejectsJwtWithAnUnexpectedAudience()`    |
| 검증   | 다른 audience의 JWT를 decoder가 거부한다. |

### M1-CRYPTO-03C

| 항목   | 내용                                                |
|--------|-----------------------------------------------------|
| 클래스 | `JwtConfigTest`                                     |
| 메서드 | `rejectsExpiredJwtWithoutClockSkew()`               |
| 검증   | 계약의 시간 경계를 벗어난 JWT를 decoder가 거부한다. |

### M1-CRYPTO-03D

| 항목   | 내용                                              |
|--------|---------------------------------------------------|
| 클래스 | `JwtConfigTest`                                   |
| 메서드 | `rejectsJwtSignedWithAnAlgorithmOtherThanHs256()` |
| 검증   | 계약과 다른 algorithm의 JWT를 decoder가 거부한다. |

### M1-CRYPTO-03E

| 항목   | 내용                                                   |
|--------|--------------------------------------------------------|
| 클래스 | `JwtConfigTest`                                        |
| 메서드 | `rejectsJwtWithForgerySignature()`                     |
| 검증   | 변조되거나 다른 key로 서명된 JWT를 decoder가 거부한다. |

### M1-CRYPTO-04A

| 항목   | 내용                                                                                            |
|--------|-------------------------------------------------------------------------------------------------|
| 클래스 | `RefreshTokenGeneratorTest`                                                                     |
| 메서드 | `generatesUniqueOpaqueBase64UrlTokensWith256BitsOfEntropy()`                                    |
| 검증 1 | 생성한 token이 계약 형식이며 두 번 생성한 값이 다르다.                                          |
| 검증 2 | SecureRandom과 생성 byte 수는 코드 검토로 확인한다. 표본 유일성으로 엔트로피를 증명하지 않는다. |

### M1-CRYPTO-04B

| 항목   | 내용                                                          |
|--------|---------------------------------------------------------------|
| 클래스 | `RefreshTokenGeneratorTest`                                   |
| 메서드 | `createsADeterministicSha256HashForDatabaseStorage()`         |
| 검증 1 | 같은 원문은 같은 SHA-256 hash가 되며 DB 저장 형식과 일치한다. |
| 검증 2 | hash가 원문과 다르다.                                         |

### M1-CRYPTO-05A

| 항목   | 내용                                    |
|--------|-----------------------------------------|
| 클래스 | `JwtPropertiesTest`                     |
| 메서드 | `failsToStartWhenTheSecretIsMissing()`  |
| 검증   | secret이 없으면 설정 초기화에 실패한다. |

### M1-CRYPTO-05B

| 항목   | 내용                                                                                            |
|--------|-------------------------------------------------------------------------------------------------|
| 클래스 | `JwtPropertiesTest`                                                                             |
| 메서드 | `failsToStartWhenTheSecretIsShorterThan32Bytes()`                                               |
| 검증   | 계약 최소 길이보다 짧은 secret이면 설정 초기화에 실패하며 secret 원문은 오류에 노출되지 않는다. |

### M1-CRYPTO-06

| 항목   | 내용                                                                            |
|--------|---------------------------------------------------------------------------------|
| 클래스 | `TokenPolicyContractTest`                                                       |
| 메서드 | `applicationTokenPolicyAndResponseConstantsMatchTheOpenApiContract()`           |
| 검증 1 | 설정의 access/refresh TTL 및 응답 token type·수명 값이 OpenAPI 정책과 일치한다. |
| 검증 2 | 실제 auth endpoint나 AuthService 구현 없이 확인한다.                            |

## 4. 인증 Application Service

application service와 이전 구성요소로 완료한다. controller와 SecurityFilterChain을 사용하지 않는다.

### M1-AUTH-01

| 항목   | 내용                                                                                      |
|--------|-------------------------------------------------------------------------------------------|
| 클래스 | `AuthServiceIntegrationTest`                                                              |
| 메서드 | `successSignUp()`                                                                         |
| 검증 1 | canonical email·비밀번호 hash를 가진 active Guardian과 refresh token hash가 저장된다.     |
| 검증 2 | 계약의 token 쌍과 Guardian 결과를 반환하고 원문 비밀번호·refresh token은 저장하지 않는다. |

### M1-AUTH-02

| 항목   | 내용                                                |
|--------|-----------------------------------------------------|
| 클래스 | `AuthServiceIntegrationTest`                        |
| 메서드 | `rejectsSignupWithSameEmail()`                      |
| 검증 1 | 정규화한 email이 같으면 email 충돌 예외가 발생한다. |
| 검증 2 | Guardian과 refresh token이 추가되지 않는다.         |

### M1-AUTH-03

| 항목   | 내용                                                                  |
|--------|-----------------------------------------------------------------------|
| 클래스 | `AuthServiceSignupRollbackTest`                                       |
| 메서드 | `rollbackWhenTransactionFailed()`                                     |
| 검증   | signup 도중 실패하면 Guardian·refresh token 저장이 함께 rollback된다. |

### M1-AUTH-04

| 항목   | 내용                                                                       |
|--------|----------------------------------------------------------------------------|
| 클래스 | `AuthServiceIntegrationTest`                                               |
| 메서드 | `successLogin()`                                                           |
| 검증   | 정규화한 email과 올바른 비밀번호로 기존 Guardian의 새 token 쌍을 발급한다. |

### M1-AUTH-05

| 항목   | 내용                                                                 |
|--------|----------------------------------------------------------------------|
| 클래스 | `AuthServiceLoginTest`                                               |
| 메서드 | `rejectsInvalidCredentialsWithoutIssuingTokens()`                    |
| 검증 1 | 없는 email과 잘못된 비밀번호는 같은 AUTH_INVALID_CREDENTIALS 결과다. |
| 검증 2 | token을 발급하지 않는다.                                             |

### M1-AUTH-06

| 항목   | 내용                                                                                       |
|--------|--------------------------------------------------------------------------------------------|
| 클래스 | `AuthServiceIntegrationTest`                                                               |
| 메서드 | `loginRejectsWithdrawnGuardianAndAllowsTemporarilyRestrictedGuardian()`                    |
| 검증   | withdrawn 계정은 자격 증명 오류로 거부하며 temporarily_restricted 계정은 login에 성공한다. |

### M1-AUTH-07

| 항목   | 내용                                                       |
|--------|------------------------------------------------------------|
| 클래스 | `AuthServiceIntegrationTest`                               |
| 메서드 | `successRefresh()`                                         |
| 검증 1 | 기존 token을 폐기하고 새 token hash와 token 쌍을 생성한다. |
| 검증 2 | 기존 폐기와 신규 저장은 같은 transaction의 결과다.         |

### M1-AUTH-08

| 항목   | 내용                                                            |
|--------|-----------------------------------------------------------------|
| 클래스 | `AuthServiceIntegrationTest`                                    |
| 메서드 | `rejectsInvalidRefreshToken()`                                  |
| 검증 1 | 만료·폐기·회전·미등록 token은 모두 AUTH_REFRESH_INVALID 결과다. |
| 검증 2 | 새 token을 만들지 않는다.                                       |

### M1-AUTH-09

| 항목   | 내용                                                           |
|--------|----------------------------------------------------------------|
| 클래스 | `AuthServiceRefreshConcurrencyIntegrationTest`                 |
| 메서드 | `allowOnlyOneConcurrencyRefreshRequest()`                      |
| 검증 1 | 같은 token의 동시 요청 중 정확히 하나만 회전에 성공한다.       |
| 검증 2 | 실패 요청은 AUTH_REFRESH_INVALID이며 활성 후속 token은 하나다. |

### M1-AUTH-10

| 항목   | 내용                                                                      |
|--------|---------------------------------------------------------------------------|
| 클래스 | `AuthServiceRefreshRollbackTest`                                          |
| 메서드 | `rollsBackRefreshTokenRotationWhenNewTokenHashViolatesUniqueConstraint()` |
| 검증 1 | 신규 token 저장 실패 시 기존 token 폐기와 신규 저장이 함께 rollback된다.  |
| 검증 2 | 기존 token 상태와 token 개수가 요청 전과 같다.                            |

### M1-AUTH-11A

| 항목   | 내용                                                  |
|--------|-------------------------------------------------------|
| 클래스 | `AuthServiceIntegrationTest`                          |
| 메서드 | `successLogoutWithValidToken()`                       |
| 검증   | 존재하는 token을 폐기하고 새 token은 생성하지 않는다. |

### M1-AUTH-11B

| 항목   | 내용                                                         |
|--------|--------------------------------------------------------------|
| 클래스 | `AuthServiceIntegrationTest`                                 |
| 메서드 | `successLogoutWithInvalidTokens()`                           |
| 검증 1 | 이미 폐기되었거나 미등록인 올바른 형식의 token에도 성공한다. |
| 검증 2 | 추가 token을 생성하지 않는다.                                |

### M1-AUTH-12

| 항목   | 내용                                                                                 |
|--------|--------------------------------------------------------------------------------------|
| 클래스 | `AuthServiceSignupConcurrencyIntegrationTest`                                        |
| 메서드 | `allowOnlyOneConcurrentSignupForSameEmail()`                                         |
| 검증 1 | 같은 canonical email의 동시 signup 중 정확히 하나만 성공한다.                        |
| 검증 2 | 다른 요청은 EmailAlreadyExistsException이며 성공한 요청의 Guardian과 token만 남는다. |

## 5. 요청 ID와 공통 REST 오류

MVC 오류는 테스트 전용 controller로, writer는 직접 직렬화로 검증한다. 실제 auth endpoint나 프로젝트 SecurityFilterChain의 완성을
기다리지 않는다.

### M1-REQ-01

| 항목   | 내용                                                             |
|--------|------------------------------------------------------------------|
| 클래스 | `RequestIdFilterMvcTest`                                         |
| 메서드 | `generatesDistinctRequestIdsForSuccessfulAndMvcErrorResponses()` |
| 검증 1 | 정상·MVC 오류 응답에 서버 생성 형식의 요청 ID가 있다.            |
| 검증 2 | 각 요청의 ID가 서로 다르다.                                      |

### M1-REQ-02

| 항목   | 내용                                                        |
|--------|-------------------------------------------------------------|
| 클래스 | `RequestIdFilterTest`                                       |
| 메서드 | `generatesServerRequestIdIgnoringIncomingRequestIdHeader()` |
| 검증 1 | 클라이언트 header 값을 재사용하지 않는다.                   |
| 검증 2 | 새 ID를 attribute·MDC·응답 header에 사용한다.               |

### M1-REQ-03

| 항목   | 내용                                                                  |
|--------|-----------------------------------------------------------------------|
| 클래스 | `RequestIdFilterTest`                                                 |
| 메서드 | `requestIdMatchesAcrossAttributeMdcHeaderAndProblemDetail()`          |
| 검증   | 같은 요청의 attribute·MDC·응답 header·ProblemDetail에 같은 ID가 있다. |

### M1-REQ-04A

| 항목   | 내용                                        |
|--------|---------------------------------------------|
| 클래스 | `RequestIdFilterTest`                       |
| 메서드 | `restoreMDCValueAfterChain()`               |
| 검증   | 정상 종료 후 이전 MDC requestId가 복원된다. |

### M1-REQ-04B

| 항목   | 내용                                                       |
|--------|------------------------------------------------------------|
| 클래스 | `RequestIdFilterTest`                                      |
| 메서드 | `removeMDCValueAfterChain()`                               |
| 검증   | 이전 MDC requestId가 없으면 정상 종료 후 새 값을 제거한다. |

### M1-REQ-04C

| 항목   | 내용                                                   |
|--------|--------------------------------------------------------|
| 클래스 | `RequestIdFilterTest`                                  |
| 메서드 | `restoreMDCValueWhileFilterThrowsException()`          |
| 검증   | 처리 중 예외가 발생해도 이전 MDC requestId가 복원된다. |

### M1-REQ-04D

| 항목   | 내용                                                           |
|--------|----------------------------------------------------------------|
| 클래스 | `RequestIdFilterTest`                                          |
| 메서드 | `removeMDCValueWhileFilterThrowsException()`                   |
| 검증   | 이전 MDC requestId가 없으면 예외 종료 후에도 새 값을 제거한다. |

### M1-ERR-01A

| 항목   | 내용                                                 |
|--------|------------------------------------------------------|
| 클래스 | `GlobalExceptionHandlerMvcTest`                      |
| 메서드 | `mapsInvalidRequestBodyToValidationFailed()`         |
| 검증 1 | 400 VALIDATION_FAILED와 name field error를 반환한다. |
| 검증 2 | reason이 비어 있지 않으며 공통 오류 검증을 만족한다. |

### M1-ERR-01B

| 항목   | 내용                                                  |
|--------|-------------------------------------------------------|
| 클래스 | `GlobalExceptionHandlerMvcTest`                       |
| 메서드 | `mapsInvalidMethodParameterToValidationFailed()`      |
| 검증 1 | 400 VALIDATION_FAILED와 count field error를 반환한다. |
| 검증 2 | reason이 비어 있지 않으며 공통 오류 검증을 만족한다.  |

### M1-ERR-01C

| 항목   | 내용                                                         |
|--------|--------------------------------------------------------------|
| 클래스 | `GlobalExceptionHandlerMvcTest`                              |
| 메서드 | `mapsMissingQueryParameterToValidationFailed()`              |
| 검증 1 | 400 VALIDATION_FAILED와 누락된 count field error를 반환한다. |
| 검증 2 | 민감한 query 값이 응답에 없으며 공통 오류 검증을 만족한다.   |

### M1-ERR-01D

| 항목   | 내용                                                                     |
|--------|--------------------------------------------------------------------------|
| 클래스 | `GlobalExceptionHandlerMvcTest`                                          |
| 메서드 | `mapsMalformedJsonToMalformedRequest()`                                  |
| 검증 1 | 400 MALFORMED_REQUEST를 반환하고 fieldErrors를 생략한다.                 |
| 검증 2 | 공통 오류 검증을 만족한다. 같은 분기의 enum·JSON 조합을 반복하지 않는다. |

### M1-ERR-01E

| 항목   | 내용                                                   |
|--------|--------------------------------------------------------|
| 클래스 | `GlobalExceptionHandlerMvcTest`                        |
| 메서드 | `mapsPathVariableTypeMismatchToValidationFailed()`     |
| 검증 1 | 400 VALIDATION_FAILED와 number field error를 반환한다. |
| 검증 2 | 공통 오류 검증을 만족한다.                             |

### M1-ERR-02A

| 항목   | 내용                                                      |
|--------|-----------------------------------------------------------|
| 클래스 | `GlobalExceptionHandlerMvcTest`                           |
| 메서드 | `mapsNoHandlerFoundToResourceNotFound()`                  |
| 검증 1 | 404 RESOURCE_NOT_FOUND를 반환하고 fieldErrors를 생략한다. |
| 검증 2 | 공통 오류 검증을 만족한다.                                |

### M1-ERR-02B

| 항목   | 내용                                                      |
|--------|-----------------------------------------------------------|
| 클래스 | `GlobalExceptionHandlerMvcTest`                           |
| 메서드 | `mapsNoResourceFoundToResourceNotFound()`                 |
| 검증 1 | 404 RESOURCE_NOT_FOUND를 반환하고 fieldErrors를 생략한다. |
| 검증 2 | 공통 오류 검증을 만족한다.                                |

### M1-ERR-02C

| 항목   | 내용                                                  |
|--------|-------------------------------------------------------|
| 클래스 | `GlobalExceptionHandlerMvcTest`                       |
| 메서드 | `mapsStateConflictBusinessException()`                |
| 검증 1 | 409 STATE_CONFLICT를 반환하고 fieldErrors를 생략한다. |
| 검증 2 | 공통 오류 검증을 만족한다.                            |

### M1-ERR-02D

| 항목   | 내용                                                        |
|--------|-------------------------------------------------------------|
| 클래스 | `GlobalExceptionHandlerMvcTest`                             |
| 메서드 | `mapsEmailAlreadyExistsException()`                         |
| 검증 1 | 409 EMAIL_ALREADY_EXISTS를 반환하고 fieldErrors를 생략한다. |
| 검증 2 | 공통 오류 검증을 만족한다.                                  |

### M1-ERR-03A

| 항목   | 내용                                                   |
|--------|--------------------------------------------------------|
| 클래스 | `GlobalExceptionHandlerMvcTest`                        |
| 메서드 | `mapsInvalidReturnValueToInternalServerError()`        |
| 검증   | 500 INTERNAL_SERVER_ERROR와 공통 오류 형식을 반환한다. |

### M1-ERR-03B

| 항목   | 내용                                                                                |
|--------|-------------------------------------------------------------------------------------|
| 클래스 | `GlobalExceptionHandlerMvcTest`                                                     |
| 메서드 | `mapsUnexpectedExceptionToInternalServerError()`                                    |
| 검증 1 | 500 INTERNAL_SERVER_ERROR와 공통 오류 형식을 반환한다.                              |
| 검증 2 | 예외에 넣은 내부 메시지·SQL·민감정보와 예외명·stack trace를 본문에 노출하지 않는다. |

### M1-ERR-04A

| 항목   | 내용                                                |
|--------|-----------------------------------------------------|
| 클래스 | `GlobalExceptionHandlerMvcTest`                     |
| 메서드 | `mapsUnsupportedMethodToMethodNotAllowed()`         |
| 검증   | 405 METHOD_NOT_ALLOWED와 공통 오류 형식을 반환한다. |

### M1-ERR-04B

| 항목   | 내용                                            |
|--------|-------------------------------------------------|
| 클래스 | `GlobalExceptionHandlerMvcTest`                 |
| 메서드 | `mapsUnsupportedAcceptHeaderToNotAcceptable()`  |
| 검증   | 406 NOT_ACCEPTABLE와 공통 오류 형식을 반환한다. |

### M1-ERR-05

| 항목   | 내용                                                    |
|--------|---------------------------------------------------------|
| 클래스 | `GlobalExceptionHandlerMvcTest`                         |
| 메서드 | `mapsUnsupportedContentTypeToUnsupportedMediaType()`    |
| 검증   | 415 UNSUPPORTED_MEDIA_TYPE와 공통 오류 형식을 반환한다. |

### M1-WRITER-01

| 항목   | 내용                                                                                                        |
|--------|-------------------------------------------------------------------------------------------------------------|
| 클래스 | `SecurityProblemDetailWriterTest`                                                                           |
| 메서드 | `writesProblemDetailToHttpResponse()`, `omitsFieldErrorsWhenThereAreNoFieldErrors()`                        |
| 검증 1 | 받은 ProblemDetail의 status·필드를 HTTP 응답에 그대로 반영하며 Content-Type은 application/problem+json이다. |
| 검증 2 | code·requestId는 JSON 최상위에 있고 fieldErrors의 존재·생략 상태를 보존한다.                                |
| 검증 3 | SecurityFilterChain 없이 현재 단계에서 완료한다. 요청 ID 생성·header 설정은 writer의 검증 대상이 아니다.    |

## 6. Stateless Security와 현재 Guardian 복원

경로 처리가 필요한 테스트는 테스트 전용 controller를 사용한다. 실제 M1 API controller는 다음 단계에서 검증하며 이 단계의 선행 조건이 아니다.

### M1-SEC-01

| 항목   | 내용                                                     |
|--------|----------------------------------------------------------|
| 클래스 | `SecurityFilterChainIntegrationTest`                     |
| 메서드 | `allowsPublicAuthRequestsWithoutAccessToken()`           |
| 검증 1 | M1의 네 공개 auth method·path를 token 없이 통과시킨다.   |
| 검증 2 | 잘못된 입력은 테스트 경로의 MVC validation까지 도달한다. |

### M1-SEC-02

| 항목   | 내용                                                                                  |
|--------|---------------------------------------------------------------------------------------|
| 클래스 | `SecurityFilterChainIntegrationTest`                                                  |
| 메서드 | `returnsProblemDetailWhenAccessTokenIsMissing()`                                      |
| 검증 1 | 보호 경로는 token 없이 접근하면 401 AUTH_TOKEN_INVALID를 반환한다.                    |
| 검증 2 | 보호된 처리는 실행되지 않으며 공통 오류 형식·header와 body의 요청 ID 일치를 만족한다. |

### M1-SEC-03A

| 항목   | 내용                                                                          |
|--------|-------------------------------------------------------------------------------|
| 클래스 | `SecurityFilterChainIntegrationTest`                                          |
| 메서드 | `rejectsTamperedAccessToken()`                                                |
| 검증 1 | 변조된 token은 401 AUTH_TOKEN_INVALID이며 Guardian 인증 객체를 만들지 않는다. |
| 검증 2 | 공통 오류 검증을 만족한다.                                                    |

### M1-SEC-03B

| 항목   | 내용                                                         |
|--------|--------------------------------------------------------------|
| 클래스 | `SecurityFilterChainIntegrationTest`                         |
| 메서드 | `rejectsMalformedGuardianSubject()`                          |
| 검증 1 | 형식이 잘못된 Guardian subject는 401 AUTH_TOKEN_INVALID이다. |
| 검증 2 | 공통 오류 검증을 만족한다.                                   |

### M1-SEC-04

| 항목   | 내용                                                               |
|--------|--------------------------------------------------------------------|
| 클래스 | `SecurityFilterChainIntegrationTest`                               |
| 메서드 | `returnsExpiredTokenProblemDetail()`                               |
| 검증 1 | 만료된 token은 401 AUTH_TOKEN_EXPIRED로 다른 인증 실패와 구분한다. |
| 검증 2 | 공통 오류 검증을 만족한다.                                         |

### M1-SEC-05

| 항목   | 내용                                                                              |
|--------|-----------------------------------------------------------------------------------|
| 클래스 | `SecurityFilterChainIntegrationTest`                                              |
| 메서드 | `restoresCurrentGuardianFromValidToken()`                                         |
| 검증 1 | active·temporarily_restricted Guardian은 인증된다.                                |
| 검증 2 | 매 요청 DB의 현재 Guardian 상태를 복원한다. 세션의 과거 상태를 재사용하지 않는다. |

### M1-SEC-06

| 항목   | 내용                                                                      |
|--------|---------------------------------------------------------------------------|
| 클래스 | `SecurityFilterChainIntegrationTest`                                      |
| 메서드 | `rejectsMissingOrWithdrawnGuardian()`                                     |
| 검증 1 | JWT subject의 Guardian이 없거나 withdrawn이면 401 AUTH_TOKEN_INVALID이다. |
| 검증 2 | 공통 오류 검증을 만족한다.                                                |

### M1-SEC-07

| 항목   | 내용                                                                        |
|--------|-----------------------------------------------------------------------------|
| 클래스 | `SecurityFilterChainIntegrationTest`                                        |
| 메서드 | `returnsProblemDetailWhenAccessIsDenied()`                                  |
| 검증 1 | 인증된 사용자의 접근 거부가 실제 filter chain에서 403 FORBIDDEN을 반환한다. |
| 검증 2 | 공통 오류 형식·header와 body의 요청 ID 일치를 만족한다.                     |

### M1-SEC-09

| 항목   | 내용                                                                    |
|--------|-------------------------------------------------------------------------|
| 클래스 | `SecurityFilterChainIntegrationTest`                                    |
| 메서드 | `authenticatesWithoutCreatingHttpSession()`                             |
| 검증 1 | 세션 cookie 없이 같은 access token으로 연속 요청을 인증한다.            |
| 검증 2 | HTTP session을 생성하지 않으며 form login·HTTP Basic을 사용하지 않는다. |

## 7. M1 API

signup → login → refresh → logout → GET me → PATCH me 순서로 연결한다. 현재 endpoint와 앞서 연결한 endpoint 또는 이전
DB 구성요소로 검증한다.

### M1-API-SU-01

| 항목   | 내용                                                                                   |
|--------|----------------------------------------------------------------------------------------|
| 클래스 | `AuthControllerIntegrationTest`                                                        |
| 메서드 | `returnsCreatedAuthResponseForSignup()`                                                |
| 검증 1 | 201 AuthResponse의 token 값·canonical email·대표 Guardian 유형의 필드가 계약과 맞는다. |
| 검증 2 | active Guardian과 refresh token hash가 저장되고 공통 성공 응답 검증을 만족한다.        |

### M1-API-SU-02A

| 항목   | 내용                                                        |
|--------|-------------------------------------------------------------|
| 클래스 | `AuthControllerIntegrationTest`                             |
| 메서드 | `rejectsInvalidSignupWithoutCreatingGuardian()`             |
| 검증 1 | 대표 조건 위반은 400 VALIDATION_FAILED다.                   |
| 검증 2 | Guardian·token이 추가되지 않으며 공통 오류 검증을 만족한다. |

### M1-API-SU-02B

| 항목   | 내용                                                        |
|--------|-------------------------------------------------------------|
| 클래스 | `AuthControllerIntegrationTest`                             |
| 메서드 | `rejectsUnknownSignupFieldWithoutCreatingGuardian()`        |
| 검증 1 | 알 수 없는 JSON 필드는 400 MALFORMED_REQUEST다.             |
| 검증 2 | Guardian·token이 추가되지 않으며 공통 오류 검증을 만족한다. |

### M1-API-SU-03

| 항목   | 내용                                                        |
|--------|-------------------------------------------------------------|
| 클래스 | `AuthControllerIntegrationTest`                             |
| 메서드 | `returnsEmailConflictForDuplicateSignup()`                  |
| 검증 1 | 같은 canonical email은 409 EMAIL_ALREADY_EXISTS다.          |
| 검증 2 | Guardian·token이 추가되지 않으며 공통 오류 검증을 만족한다. |

### M1-API-LI-01

| 항목   | 내용                                                                              |
|--------|-----------------------------------------------------------------------------------|
| 클래스 | `AuthControllerIntegrationTest`                                                   |
| 메서드 | `returnsAuthResponseForValidLogin()`                                              |
| 검증 1 | 200 AuthResponse의 Guardian·token이 계약과 맞고 새 refresh token hash가 저장된다. |
| 검증 2 | 공통 성공 응답 검증을 만족한다.                                                   |

### M1-API-LI-02

| 항목   | 내용                                                                                              |
|--------|---------------------------------------------------------------------------------------------------|
| 클래스 | `AuthControllerIntegrationTest`                                                                   |
| 메서드 | `returnsSameErrorForInvalidLoginCredentials()`                                                    |
| 검증 1 | 없는 email과 잘못된 비밀번호는 같은 401 AUTH_INVALID_CREDENTIALS다.                               |
| 검증 2 | 유효 길이의 단순 비밀번호를 signup 복잡도 규칙으로 400 거부하지 않으며 공통 오류 검증을 만족한다. |

### M1-API-RF-01

| 항목   | 내용                                                                               |
|--------|------------------------------------------------------------------------------------|
| 클래스 | `AuthControllerIntegrationTest`                                                    |
| 메서드 | `rotatesRefreshTokenAndRejectsReuse()`                                             |
| 검증 1 | 첫 요청은 200 새 TokenResponse이고 이전 token 재사용은 401 AUTH_REFRESH_INVALID다. |
| 검증 2 | 성공·오류 공통 응답 검증을 만족한다.                                               |

### M1-API-RF-02A

| 항목   | 내용                                                                   |
|--------|------------------------------------------------------------------------|
| 클래스 | `AuthControllerIntegrationTest`                                        |
| 메서드 | `returnsValidationErrorForMalformedRefreshToken()`                     |
| 검증   | 형식 오류 token은 400 VALIDATION_FAILED이며 공통 오류 검증을 만족한다. |

### M1-API-RF-02B

| 항목   | 내용                                                                                 |
|--------|--------------------------------------------------------------------------------------|
| 클래스 | `AuthControllerIntegrationTest`                                                      |
| 메서드 | `returnsInvalidRefreshForUnknownToken()`                                             |
| 검증   | 올바른 형식의 미등록 token은 401 AUTH_REFRESH_INVALID이며 공통 오류 검증을 만족한다. |

### M1-API-LO-01

| 항목   | 내용                                                        |
|--------|-------------------------------------------------------------|
| 클래스 | `AuthControllerIntegrationTest`                             |
| 메서드 | `returnsNoContentForRepeatedLogout()`                       |
| 검증 1 | 최초·반복 logout은 모두 body 없는 204다.                    |
| 검증 2 | 이후 기존 token의 refresh는 거부되며 응답에 요청 ID가 있다. |

### M1-API-ME-01

| 항목   | 내용                                                                                                                    |
|--------|-------------------------------------------------------------------------------------------------------------------------|
| 클래스 | `GuardianControllerIntegrationTest`                                                                                     |
| 메서드 | `returnsCurrentGuardianForEachProfileType()`                                                                            |
| 검증 1 | 유효 token으로 조회하면 200 Guardian 응답이다.                                                                          |
| 검증 2 | 개인·커플·가족 discriminator와 유형별 gender, canonical email·현재 status가 계약과 맞고 공통 성공 응답 검증을 만족한다. |

### M1-API-ME-02

| 항목   | 내용                                                                               |
|--------|------------------------------------------------------------------------------------|
| 클래스 | `GuardianControllerIntegrationTest`                                                |
| 메서드 | `rejectsUnauthenticatedMeRequest()`                                                |
| 검증 1 | 실제 GET /api/guardians/me에 token이 없으면 401 AUTH_TOKEN_INVALID다.              |
| 검증 2 | 공통 오류 검증을 만족한다. Security 단계의 상세 token 실패 조합은 반복하지 않는다. |

### M1-API-ME-03

| 항목   | 내용                                                        |
|--------|-------------------------------------------------------------|
| 클래스 | `GuardianControllerIntegrationTest`                         |
| 메서드 | `updatesCurrentGuardianWithValidProfile()`                  |
| 검증 1 | 유효한 유형·성별·공개 설정 변경이 현재 Guardian에 저장된다. |
| 검증 2 | 200 응답과 DB 상태가 같고 공통 성공 응답 검증을 만족한다.   |

### M1-API-ME-04A

| 항목   | 내용                                                       |
|--------|------------------------------------------------------------|
| 클래스 | `GuardianControllerIntegrationTest`                        |
| 메서드 | `preservesGuardianWhenUpdateValidationFails()`             |
| 검증 1 | 대표 조건 위반은 400 VALIDATION_FAILED다.                  |
| 검증 2 | Guardian의 기존 상태가 유지되며 공통 오류 검증을 만족한다. |

### M1-API-ME-04B

| 항목   | 내용                                                       |
|--------|------------------------------------------------------------|
| 클래스 | `GuardianControllerIntegrationTest`                        |
| 메서드 | `preservesGuardianWhenUpdateDeserializationFails()`        |
| 검증 1 | 대표 역직렬화 실패는 400 MALFORMED_REQUEST다.              |
| 검증 2 | Guardian의 기존 상태가 유지되며 공통 오류 검증을 만족한다. |

### M1-API-ME-05

| 항목   | 내용                                                          |
|--------|---------------------------------------------------------------|
| 클래스 | `GuardianControllerIntegrationTest`                           |
| 메서드 | `updatesOnlyAuthenticatedGuardian()`                          |
| 검증 1 | 다른 Guardian이 존재해도 token subject의 Guardian만 수정된다. |
| 검증 2 | 다른 Guardian의 상태가 유지된다.                              |

## 8. 계약·보안 회귀와 수동 흐름

모든 구현 단계가 끝난 뒤 수행하는 확인 절차다. 각 기능의 TDD마다 새 검증 도구나 테스트 메서드를 작성하지 않는다.

### M1-REG-01

| 항목 | 내용                                                                                   |
|------|----------------------------------------------------------------------------------------|
| 확인 | OpenAPI validator·client type 생성 실행                                                |
| 검증 | M1 요청·응답과 Guardian discriminator·ProblemDetail 확장 필드를 도구가 처리할 수 있다. |

### M1-REG-02

| 항목 | 내용                                                                        |
|------|-----------------------------------------------------------------------------|
| 확인 | 전체 테스트 로그·실패 보고서 확인                                           |
| 검증 | 비밀번호·token 원문·내부 예외·SQL 및 fixture secret의 노출 여부를 확인한다. |

### M1-REG-03

| 항목 | 내용                                                            |
|------|-----------------------------------------------------------------|
| 확인 | ./gradlew check 실행                                            |
| 검증 | 현재 코드의 단위·PostgreSQL 통합·API·형식 검사가 모두 통과한다. |

### M1-MAN-01

| 항목 | 내용                                                              |
|------|-------------------------------------------------------------------|
| 확인 | signup → GET/PATCH me → refresh → 이전 token 재사용 → logout 확인 |
| 검증 | 각 응답의 status·body·Content-Type·요청 ID가 계약과 일치한다.     |

## 완료 판정

완료 여부는 [M1 인증 계약 체크리스트](./m1-auth-contract-checklist.md)에서 관리한다. ID 재분류만으로 미검증 항목을 완료로 바꾸지 않는다.
M1 종료 시 전체 회귀·계약 확인·수동 흐름과 사람 검토를 마친다.
