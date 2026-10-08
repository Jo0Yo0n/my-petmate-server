# M1 테스트 케이스

이 문서는 핵심 테스트 ID와 검증 목적의 목록이다. 각 ID의 대응 메서드와 검증 결과는 [상세 문서](./m1-test-cases-detail.md)에서 확인한다.
완료 상태는 [M1 인증 계약 체크리스트](./m1-auth-contract-checklist.md)에서만 관리한다.

라이브러리의 기본 동작과 이미 확인한 상세 입력 조합은 필수 작성 범위에 포함하지 않는다. 기존 보강 테스트는 유지한다.

## 실행 순서

- 체크리스트의 구현 순서를 따른다. 각 ID는 테스트 메서드 하나에 대응하며, 같은 결과에 입력만 다른 경우 parameterized 메서드 하나로 검증한다.
- 기존 메서드가 대응하면 재작성하지 않는다. 공통 assertion은 기존 테스트에 적용하고 별도 테스트 ID로 만들지 않는다.
- 현재 단계와 이미 완료한 구성요소만 사용한다. 이후 단계의 구현이나 테스트 통과를 현재 단계의 완료 조건으로 삼지 않는다.
- 핵심 결과를 검증하면 다음 구현으로 진행한다. 새로운 분기·경쟁 조건·부분 실패 또는 확인된 결함이 있을 때 보강한다.

### 1. 입력 모델과 계약

| ID           | 검증 목적                     | 상세                                              |
|--------------|-------------------------------|---------------------------------------------------|
| `M1-DTO-01A` | enum JSON 출력값              | [검증 항목](./m1-test-cases-detail.md#m1-dto-01a) |
| `M1-DTO-01B` | enum JSON 입력값              | [검증 항목](./m1-test-cases-detail.md#m1-dto-01b) |
| `M1-DTO-01C` | 잘못된 enum 입력 거부         | [검증 항목](./m1-test-cases-detail.md#m1-dto-01c) |
| `M1-DTO-01D` | enum DB 변환                  | [검증 항목](./m1-test-cases-detail.md#m1-dto-01d) |
| `M1-DTO-02A` | 유효한 보호자 유형 조합       | [검증 항목](./m1-test-cases-detail.md#m1-dto-02a) |
| `M1-DTO-02B` | 개인의 성별 필수              | [검증 항목](./m1-test-cases-detail.md#m1-dto-02b) |
| `M1-DTO-02C` | 커플·가족의 성별 금지         | [검증 항목](./m1-test-cases-detail.md#m1-dto-02c) |
| `M1-DTO-02D` | 보호자 수정 입력 조건         | [검증 항목](./m1-test-cases-detail.md#m1-dto-02d) |
| `M1-DTO-03A` | 비밀번호 필수 문자군 허용     | [검증 항목](./m1-test-cases-detail.md#m1-dto-03a) |
| `M1-DTO-03B` | 비밀번호 필수 문자군 누락     | [검증 항목](./m1-test-cases-detail.md#m1-dto-03b) |
| `M1-DTO-03C` | 비밀번호 길이·문자 경계       | [검증 항목](./m1-test-cases-detail.md#m1-dto-03c) |
| `M1-DTO-03D` | 빈 signup 비밀번호 거부       | [검증 항목](./m1-test-cases-detail.md#m1-dto-03d) |
| `M1-DTO-04`  | 입력 email 정규화             | [검증 항목](./m1-test-cases-detail.md#m1-dto-04)  |
| `M1-DTO-05A` | email 공백·길이 경계          | [검증 항목](./m1-test-cases-detail.md#m1-dto-05a) |
| `M1-DTO-05B` | email 비허용 문자             | [검증 항목](./m1-test-cases-detail.md#m1-dto-05b) |
| `M1-DTO-05C` | signup 필수 입력·email 형식   | [검증 항목](./m1-test-cases-detail.md#m1-dto-05c) |
| `M1-DTO-06A` | login의 단순 비밀번호 허용    | [검증 항목](./m1-test-cases-detail.md#m1-dto-06a) |
| `M1-DTO-06B` | login 비밀번호 누락           | [검증 항목](./m1-test-cases-detail.md#m1-dto-06b) |
| `M1-DTO-06C` | login 비밀번호 빈 값          | [검증 항목](./m1-test-cases-detail.md#m1-dto-06c) |
| `M1-DTO-06D` | login 비밀번호 최대 길이      | [검증 항목](./m1-test-cases-detail.md#m1-dto-06d) |
| `M1-DTO-07A` | 유효한 refresh 입력 형식      | [검증 항목](./m1-test-cases-detail.md#m1-dto-07a) |
| `M1-DTO-07B` | 짧은 refresh 입력             | [검증 항목](./m1-test-cases-detail.md#m1-dto-07b) |
| `M1-DTO-07C` | 긴 refresh 입력               | [검증 항목](./m1-test-cases-detail.md#m1-dto-07c) |
| `M1-DTO-07D` | refresh 비허용 문자           | [검증 항목](./m1-test-cases-detail.md#m1-dto-07d) |
| `M1-DTO-07E` | refresh 입력 누락             | [검증 항목](./m1-test-cases-detail.md#m1-dto-07e) |
| `M1-DTO-07F` | refresh 입력 빈 값            | [검증 항목](./m1-test-cases-detail.md#m1-dto-07f) |
| `M1-DTO-08A` | signup의 알 수 없는 필드      | [검증 항목](./m1-test-cases-detail.md#m1-dto-08a) |
| `M1-DTO-08B` | login의 알 수 없는 필드       | [검증 항목](./m1-test-cases-detail.md#m1-dto-08b) |
| `M1-DTO-08C` | refresh의 알 수 없는 필드     | [검증 항목](./m1-test-cases-detail.md#m1-dto-08c) |
| `M1-DTO-08D` | 알 수 없는 profileType        | [검증 항목](./m1-test-cases-detail.md#m1-dto-08d) |

### 2. Migration과 JPA 영속성

| ID          | 검증 목적               | 상세                                             |
|-------------|-------------------------|--------------------------------------------------|
| `M1-DB-01`  | migration 적용          | [검증 항목](./m1-test-cases-detail.md#m1-db-01)  |
| `M1-DB-02A` | 유효한 DB 보호자 조합   | [검증 항목](./m1-test-cases-detail.md#m1-db-02a) |
| `M1-DB-02B` | 잘못된 DB 보호자 조합   | [검증 항목](./m1-test-cases-detail.md#m1-db-02b) |
| `M1-DB-03`  | email DB 유일성         | [검증 항목](./m1-test-cases-detail.md#m1-db-03)  |
| `M1-DB-04`  | refresh token DB 제약   | [검증 항목](./m1-test-cases-detail.md#m1-db-04)  |
| `M1-DB-05`  | Guardian 삭제의 FK 정책 | [검증 항목](./m1-test-cases-detail.md#m1-db-05)  |
| `M1-JPA-01` | Guardian 저장·복원      | [검증 항목](./m1-test-cases-detail.md#m1-jpa-01) |
| `M1-JPA-02` | RefreshToken 저장·복원  | [검증 항목](./m1-test-cases-detail.md#m1-jpa-02) |
| `M1-JPA-03` | token row lock          | [검증 항목](./m1-test-cases-detail.md#m1-jpa-03) |

### 3. 비밀번호와 Token 구성요소

| ID              | 검증 목적                      | 상세                                                 |
|-----------------|--------------------------------|------------------------------------------------------|
| `M1-CRYPTO-01A` | 비밀번호 hash와 일치           | [검증 항목](./m1-test-cases-detail.md#m1-crypto-01a) |
| `M1-CRYPTO-02A` | JWT 발급 claim                 | [검증 항목](./m1-test-cases-detail.md#m1-crypto-02a) |
| `M1-CRYPTO-02B` | JWT encoder·decoder 연결       | [검증 항목](./m1-test-cases-detail.md#m1-crypto-02b) |
| `M1-CRYPTO-03A` | 잘못된 issuer                  | [검증 항목](./m1-test-cases-detail.md#m1-crypto-03a) |
| `M1-CRYPTO-03B` | 잘못된 audience                | [검증 항목](./m1-test-cases-detail.md#m1-crypto-03b) |
| `M1-CRYPTO-03C` | 만료된 JWT                     | [검증 항목](./m1-test-cases-detail.md#m1-crypto-03c) |
| `M1-CRYPTO-03D` | 잘못된 algorithm               | [검증 항목](./m1-test-cases-detail.md#m1-crypto-03d) |
| `M1-CRYPTO-03E` | 변조된 JWT 서명                | [검증 항목](./m1-test-cases-detail.md#m1-crypto-03e) |
| `M1-CRYPTO-04A` | refresh token 생성             | [검증 항목](./m1-test-cases-detail.md#m1-crypto-04a) |
| `M1-CRYPTO-04B` | refresh token hash             | [검증 항목](./m1-test-cases-detail.md#m1-crypto-04b) |
| `M1-CRYPTO-05A` | JWT secret 누락                | [검증 항목](./m1-test-cases-detail.md#m1-crypto-05a) |
| `M1-CRYPTO-05B` | 짧은 JWT secret                | [검증 항목](./m1-test-cases-detail.md#m1-crypto-05b) |
| `M1-CRYPTO-06`  | token 정책 일치                | [검증 항목](./m1-test-cases-detail.md#m1-crypto-06)  |

### 4. 인증 Application Service

| ID            | 검증 목적                  | 상세                                               |
|---------------|----------------------------|----------------------------------------------------|
| `M1-AUTH-01`  | signup 정상 처리           | [검증 항목](./m1-test-cases-detail.md#m1-auth-01)  |
| `M1-AUTH-02`  | signup email 충돌          | [검증 항목](./m1-test-cases-detail.md#m1-auth-02)  |
| `M1-AUTH-03`  | signup 부분 실패 rollback  | [검증 항목](./m1-test-cases-detail.md#m1-auth-03)  |
| `M1-AUTH-04`  | login 정상 처리            | [검증 항목](./m1-test-cases-detail.md#m1-auth-04)  |
| `M1-AUTH-05`  | login 자격 증명 실패       | [검증 항목](./m1-test-cases-detail.md#m1-auth-05)  |
| `M1-AUTH-06`  | 계정 상태별 login          | [검증 항목](./m1-test-cases-detail.md#m1-auth-06)  |
| `M1-AUTH-07`  | refresh 회전 정상 처리     | [검증 항목](./m1-test-cases-detail.md#m1-auth-07)  |
| `M1-AUTH-08`  | 유효하지 않은 refresh 거부 | [검증 항목](./m1-test-cases-detail.md#m1-auth-08)  |
| `M1-AUTH-09`  | 동시 refresh               | [검증 항목](./m1-test-cases-detail.md#m1-auth-09)  |
| `M1-AUTH-10`  | refresh 부분 실패 rollback | [검증 항목](./m1-test-cases-detail.md#m1-auth-10)  |
| `M1-AUTH-11A` | 유효 token logout          | [검증 항목](./m1-test-cases-detail.md#m1-auth-11a) |
| `M1-AUTH-11B` | 반복·미등록 token logout   | [검증 항목](./m1-test-cases-detail.md#m1-auth-11b) |
| `M1-AUTH-12`  | 동시 signup                | [검증 항목](./m1-test-cases-detail.md#m1-auth-12)  |

### 5. 요청 ID와 공통 REST 오류

| ID             | 검증 목적                        | 상세                                                |
|----------------|----------------------------------|-----------------------------------------------------|
| `M1-REQ-01`    | 응답 요청 ID 형식·유일성         | [검증 항목](./m1-test-cases-detail.md#m1-req-01)    |
| `M1-REQ-02`    | 외부 요청 ID 무시                | [검증 항목](./m1-test-cases-detail.md#m1-req-02)    |
| `M1-REQ-03`    | 요청 ID 전달 일치                | [검증 항목](./m1-test-cases-detail.md#m1-req-03)    |
| `M1-REQ-04A`   | 정상 종료의 기존 MDC 복원        | [검증 항목](./m1-test-cases-detail.md#m1-req-04a)   |
| `M1-REQ-04B`   | 정상 종료의 MDC 제거             | [검증 항목](./m1-test-cases-detail.md#m1-req-04b)   |
| `M1-REQ-04C`   | 예외 종료의 기존 MDC 복원        | [검증 항목](./m1-test-cases-detail.md#m1-req-04c)   |
| `M1-REQ-04D`   | 예외 종료의 MDC 제거             | [검증 항목](./m1-test-cases-detail.md#m1-req-04d)   |
| `M1-ERR-01A`   | body validation 오류             | [검증 항목](./m1-test-cases-detail.md#m1-err-01a)   |
| `M1-ERR-01B`   | method parameter validation 오류 | [검증 항목](./m1-test-cases-detail.md#m1-err-01b)   |
| `M1-ERR-01C`   | 필수 query parameter 누락        | [검증 항목](./m1-test-cases-detail.md#m1-err-01c)   |
| `M1-ERR-01D`   | JSON 역직렬화 오류               | [검증 항목](./m1-test-cases-detail.md#m1-err-01d)   |
| `M1-ERR-01E`   | path variable 타입 오류          | [검증 항목](./m1-test-cases-detail.md#m1-err-01e)   |
| `M1-ERR-02A`   | handler 없음                     | [검증 항목](./m1-test-cases-detail.md#m1-err-02a)   |
| `M1-ERR-02B`   | resource 없음                    | [검증 항목](./m1-test-cases-detail.md#m1-err-02b)   |
| `M1-ERR-02C`   | 상태 충돌                        | [검증 항목](./m1-test-cases-detail.md#m1-err-02c)   |
| `M1-ERR-02D`   | email 중복                       | [검증 항목](./m1-test-cases-detail.md#m1-err-02d)   |
| `M1-ERR-03A`   | 반환값 validation 오류           | [검증 항목](./m1-test-cases-detail.md#m1-err-03a)   |
| `M1-ERR-03B`   | 예상하지 못한 오류               | [검증 항목](./m1-test-cases-detail.md#m1-err-03b)   |
| `M1-ERR-04A`   | 지원하지 않는 HTTP method        | [검증 항목](./m1-test-cases-detail.md#m1-err-04a)   |
| `M1-ERR-04B`   | 지원하지 않는 Accept             | [검증 항목](./m1-test-cases-detail.md#m1-err-04b)   |
| `M1-ERR-05`    | 지원하지 않는 요청 Content-Type  | [검증 항목](./m1-test-cases-detail.md#m1-err-05)    |
| `M1-WRITER-01` | ProblemDetail HTTP 직렬화        | [검증 항목](./m1-test-cases-detail.md#m1-writer-01) |

### 6. Stateless Security와 현재 Guardian 복원

| ID           | 검증 목적               | 상세                                              |
|--------------|-------------------------|---------------------------------------------------|
| `M1-SEC-01`  | 공개 auth 경로          | [검증 항목](./m1-test-cases-detail.md#m1-sec-01)  |
| `M1-SEC-02`  | 보호 경로의 token 누락  | [검증 항목](./m1-test-cases-detail.md#m1-sec-02)  |
| `M1-SEC-03A` | 변조된 token            | [검증 항목](./m1-test-cases-detail.md#m1-sec-03a) |
| `M1-SEC-03B` | 잘못된 JWT subject      | [검증 항목](./m1-test-cases-detail.md#m1-sec-03b) |
| `M1-SEC-04`  | 만료된 access token     | [검증 항목](./m1-test-cases-detail.md#m1-sec-04)  |
| `M1-SEC-05`  | 현재 Guardian 복원      | [검증 항목](./m1-test-cases-detail.md#m1-sec-05)  |
| `M1-SEC-06`  | 인증할 수 없는 Guardian | [검증 항목](./m1-test-cases-detail.md#m1-sec-06)  |
| `M1-SEC-07`  | 접근 거부의 writer 연결 | [검증 항목](./m1-test-cases-detail.md#m1-sec-07)  |
| `M1-SEC-09`  | stateless 인증          | [검증 항목](./m1-test-cases-detail.md#m1-sec-09)  |

### 7. M1 API

| ID              | 검증 목적                        | 상세                                                 |
|-----------------|----------------------------------|------------------------------------------------------|
| `M1-API-SU-01`  | signup 정상 응답                 | [검증 항목](./m1-test-cases-detail.md#m1-api-su-01)  |
| `M1-API-SU-02A` | signup validation 연결           | [검증 항목](./m1-test-cases-detail.md#m1-api-su-02a) |
| `M1-API-SU-02B` | signup JSON 오류 연결            | [검증 항목](./m1-test-cases-detail.md#m1-api-su-02b) |
| `M1-API-SU-03`  | signup email 충돌 연결           | [검증 항목](./m1-test-cases-detail.md#m1-api-su-03)  |
| `M1-API-LI-01`  | login 정상 응답                  | [검증 항목](./m1-test-cases-detail.md#m1-api-li-01)  |
| `M1-API-LI-02`  | login 인증 실패 연결             | [검증 항목](./m1-test-cases-detail.md#m1-api-li-02)  |
| `M1-API-RF-01`  | refresh 회전·재사용 거부 연결    | [검증 항목](./m1-test-cases-detail.md#m1-api-rf-01)  |
| `M1-API-RF-02A` | refresh 입력 validation 연결     | [검증 항목](./m1-test-cases-detail.md#m1-api-rf-02a) |
| `M1-API-RF-02B` | 미등록 refresh 연결              | [검증 항목](./m1-test-cases-detail.md#m1-api-rf-02b) |
| `M1-API-LO-01`  | logout 멱등 응답 연결            | [검증 항목](./m1-test-cases-detail.md#m1-api-lo-01)  |
| `M1-API-ME-01`  | 현재 Guardian 조회 응답          | [검증 항목](./m1-test-cases-detail.md#m1-api-me-01)  |
| `M1-API-ME-02`  | 실제 me 경로 보호 연결           | [검증 항목](./m1-test-cases-detail.md#m1-api-me-02)  |
| `M1-API-ME-03`  | 현재 Guardian 수정 응답          | [검증 항목](./m1-test-cases-detail.md#m1-api-me-03)  |
| `M1-API-ME-04A` | 수정 validation 실패의 상태 보존 | [검증 항목](./m1-test-cases-detail.md#m1-api-me-04a) |
| `M1-API-ME-04B` | 수정 JSON 오류의 상태 보존       | [검증 항목](./m1-test-cases-detail.md#m1-api-me-04b) |
| `M1-API-ME-05`  | 사용자 간 수정 권한 경계         | [검증 항목](./m1-test-cases-detail.md#m1-api-me-05)  |

### 8. 계약·보안 회귀와 수동 흐름

다음 항목은 M1 종료 확인 절차이며 테스트 메서드를 추가하는 작업이 아니다.

| ID          | 검증 목적                    | 상세                                             |
|-------------|------------------------------|--------------------------------------------------|
| `M1-REG-01` | OpenAPI 소비 가능성 확인     | [검증 항목](./m1-test-cases-detail.md#m1-reg-01) |
| `M1-REG-02` | 로그·보고서의 정보 노출 확인 | [검증 항목](./m1-test-cases-detail.md#m1-reg-02) |
| `M1-REG-03` | 전체 회귀·형식 확인          | [검증 항목](./m1-test-cases-detail.md#m1-reg-03) |
| `M1-MAN-01` | 실제 인증 사용자 흐름 확인   | [검증 항목](./m1-test-cases-detail.md#m1-man-01) |
