# My Petmate REST 오류 응답 계약

## 목적

My Petmate REST API는 Spring Framework가 지원하는 RFC 9457 `ProblemDetail` 형식으로 오류를 반환한다.
응답의 `Content-Type`은 `application/problem+json`이다. 표준 필드에 모바일 앱의 분기와 운영 추적에
필요한 `code`, `fieldErrors`, `requestId` 확장 필드를 추가한다.

WebSocket 오류는 frame event이므로 `docs/websocket-protocol.md`의 `message_error` 계약을 사용한다.

## 필드 역할

| 필드 | 구분 | 필수 | 역할 | 예시 |
| --- | --- | --- | --- | --- |
| `type` | RFC 9457 | 예 | 오류 종류를 나타내는 안정적인 URI다. 같은 `code`는 항상 같은 `type`을 사용한다. | `urn:my-petmate:problem:validation-failed` |
| `title` | RFC 9457 | 예 | 오류 종류의 짧고 안정적인 제목이다. 요청별 세부 내용은 넣지 않는다. | `Validation failed` |
| `status` | RFC 9457 | 예 | 실제 HTTP status와 같은 숫자다. 앱의 큰 오류 범주 분류에 사용한다. | `400` |
| `detail` | RFC 9457 | 예 | 이번 요청이 실패한 구체적인 이유다. 사용자에게 표시할 수 있지만 앱 분기에는 사용하지 않는다. | `요청값을 확인해 주세요.` |
| `instance` | RFC 9457 | 예 | 오류가 발생한 요청 경로다. 비밀번호, token 및 민감한 query 값은 포함하지 않는다. | `/api/guardians/me` |
| `code` | My Petmate 확장 | 예 | 앱이 refresh, 화면 이탈, 필터 안내 등 구체적인 동작을 결정하는 안정적인 enum이다. | `VALIDATION_FAILED` |
| `fieldErrors` | My Petmate 확장 | 아니오 | 입력 validation 실패 시 필드별 오류를 표시한다. validation이 아니면 생략한다. | `[{"field":"gender","reason":"필수입니다."}]` |
| `requestId` | My Petmate 확장 | 예 | 서버 로그와 사용자 문의를 연결하는 서버 생성 요청 추적 식별자다. | `req-01ARZ3NDEKTSV4RRFFQ69G5FAV` |

앱은 `detail` 문자열을 비교해서 분기하지 않는다. HTTP `status`로 인증·입력·권한·서버 오류의 큰
범주를 판단하고, 구체적인 동작은 `code`로 결정한다.

## requestId 생성과 전달

- 모든 HTTP 요청마다 서버가 새 requestId를 하나 생성한다. 형식은 `req-`와 26자 Crockford Base32
  ULID이며 정규식 `^req-[0-7][0-9A-HJKMNP-TV-Z]{25}$`을 만족한다. 첫 문자를 `0`~`7`로
  제한해 canonical ULID의 128비트 범위를 보장한다.
- 클라이언트가 `X-Request-Id`를 보내더라도 서버는 그 값을 신뢰하거나 재사용하지 않는다.
- 생성 filter는 Spring Security보다 먼저 실행한다. 따라서 MVC 오류뿐 아니라 Security의 401·403도
  같은 requestId를 사용할 수 있다.
- 같은 값을 request attribute와 MDC key `requestId`에 저장하고 모든 성공·오류 응답의
  `X-Request-Id` header에 반환한다.
- 오류 응답에서는 header의 `X-Request-Id`와 ProblemDetail의 `requestId`가 반드시 같아야 한다.
- 요청 처리가 끝나면 `finally`에서 MDC 값을 제거해 thread가 재사용될 때 다른 요청의 ID가 섞이지
  않게 한다.
- requestId에는 사용자, token, 리소스 식별 정보를 넣지 않는다. requestId는 로그 상관관계용이며
  인증 수단이나 요청 멱등성 키로 사용하지 않는다.

## 기본 예시

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json
X-Request-Id: req-01ARZ3NDEKTSV4RRFFQ69G5FAV
```

```json
{
  "type": "urn:my-petmate:problem:validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "요청값을 확인해 주세요.",
  "instance": "/api/guardians/me",
  "code": "VALIDATION_FAILED",
  "fieldErrors": [
    {
      "field": "gender",
      "reason": "profileType이 individual이면 필수입니다."
    }
  ],
  "requestId": "req-01ARZ3NDEKTSV4RRFFQ69G5FAV"
}
```

## HTTP status 사용 원칙

| Status | 의미 | My Petmate 사용 예 |
| --- | --- | --- |
| `400 Bad Request` | 요청값 형식 또는 필드 조합이 잘못됨 | validation, 비공개 보호자의 identity 필터 사용 |
| `401 Unauthorized` | 로그인 자격 증명 또는 token이 유효하지 않음 | 로그인 실패, access/refresh token 만료·오류 |
| `403 Forbidden` | 인증됐지만 리소스나 기능 사용 권한이 없음 | 다른 보호자의 dog 접근, 제한 계정의 추천·채팅 |
| `404 Not Found` | 요청한 리소스가 없음 | dog, match, appointment를 찾을 수 없음 |
| `405 Method Not Allowed` | endpoint가 HTTP method를 지원하지 않음 | GET 전용 endpoint에 POST |
| `406 Not Acceptable` | 요청한 응답 media type을 만들 수 없음 | 지원하지 않는 `Accept` 헤더 |
| `409 Conflict` | 현재 리소스 상태 또는 중복 요청과 충돌 | 종료된 match, 잘못된 appointment 상태, 중복 평가 |
| `413 Payload Too Large` | 업로드 body가 제한을 초과함 | 10 MiB 초과 dog 사진 |
| `415 Unsupported Media Type` | 요청 또는 파일 media type을 지원하지 않음 | JPEG·PNG·WebP가 아닌 dog 사진 |
| `500 Internal Server Error` | 예상하지 못한 서버 오류 | 내부 예외. 예외명과 stack trace는 응답하지 않음 |

## 에러 코드

| HTTP status | `code` | `type` | 사용 조건 및 앱 동작 |
| --- | --- | --- | --- |
| 400 | `VALIDATION_FAILED` | `urn:my-petmate:problem:validation-failed` | body/query/path validation 실패. `fieldErrors`를 폼에 표시한다. |
| 400 | `IDENTITY_DISCLOSURE_REQUIRED` | `urn:my-petmate:problem:identity-disclosure-required` | 비공개 보호자가 `guardianIdentities`를 사용했다. 공개 설정 안내를 표시한다. |
| 401 | `AUTH_TOKEN_EXPIRED` | `urn:my-petmate:problem:auth-token-expired` | access token 만료. refresh를 한 번 수행하고 원 요청을 한 번 재시도한다. |
| 401 | `AUTH_TOKEN_INVALID` | `urn:my-petmate:problem:auth-token-invalid` | access token 누락·변조·오류. 자동 재시도하지 않고 로그인 상태를 정리한다. |
| 401 | `AUTH_INVALID_CREDENTIALS` | `urn:my-petmate:problem:invalid-credentials` | 로그인 이메일 또는 비밀번호 오류. 어느 값이 틀렸는지는 구분해 노출하지 않는다. |
| 401 | `AUTH_REFRESH_INVALID` | `urn:my-petmate:problem:refresh-token-invalid` | refresh token 만료·폐기·재사용·오류. token을 삭제하고 로그인 화면으로 이동한다. |
| 403 | `FORBIDDEN` | `urn:my-petmate:problem:forbidden` | 소유권 또는 참여자 권한 없음. |
| 403 | `ACCOUNT_RESTRICTED` | `urn:my-petmate:problem:account-restricted` | 임시 제한 계정이 추천 또는 채팅을 요청했다. 제한 안내를 표시한다. |
| 404 | `RESOURCE_NOT_FOUND` | `urn:my-petmate:problem:resource-not-found` | 대상 리소스가 없거나 공개하지 않기로 한 리소스다. |
| 409 | `EMAIL_ALREADY_EXISTS` | `urn:my-petmate:problem:email-already-exists` | 이미 가입된 이메일이다. 회원가입 이메일 필드에 표시한다. |
| 409 | `STATE_CONFLICT` | `urn:my-petmate:problem:state-conflict` | 별도 도메인 code가 없는 일반 상태·중복 충돌이다. |
| 409 | `MATCH_CLOSED` | `urn:my-petmate:problem:match-closed` | 종료된 match 접근. 채팅 입력을 닫고 목록으로 이동한다. |
| 409 | `MATCH_BLOCKED` | `urn:my-petmate:problem:match-blocked` | 차단된 match 접근. 채팅 입력을 닫고 목록으로 이동한다. |
| 409 | `APPOINTMENT_STATE_CONFLICT` | `urn:my-petmate:problem:appointment-state-conflict` | 현재 약속 상태에서 해당 전이를 할 수 없다. 약속 카드를 다시 조회한다. |
| 409 | `REVIEW_NOT_ELIGIBLE` | `urn:my-petmate:problem:review-not-eligible` | 아직 종료되지 않았거나 accepted가 아닌 약속 평가다. |
| 409 | `REVIEW_ALREADY_SUBMITTED` | `urn:my-petmate:problem:review-already-submitted` | 참여자가 같은 약속에 이미 평가를 제출했다. 제출 완료 상태로 갱신한다. |
| 413 | `PAYLOAD_TOO_LARGE` | `urn:my-petmate:problem:payload-too-large` | dog 사진 크기 제한 초과. 사진을 줄이도록 안내한다. |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | `urn:my-petmate:problem:unsupported-media-type` | 요청 또는 사진 형식이 지원되지 않는다. |
| 405 | `METHOD_NOT_ALLOWED` | `urn:my-petmate:problem:method-not-allowed` | 클라이언트 계약 오류이므로 사용자 재시도 대상이 아니다. |
| 406 | `NOT_ACCEPTABLE` | `urn:my-petmate:problem:not-acceptable` | 클라이언트가 지원하지 않는 응답 형식을 요구했다. |
| 500 | `INTERNAL_SERVER_ERROR` | `urn:my-petmate:problem:internal-server-error` | 일반 오류 화면과 재시도를 제공하고 `requestId`를 기록한다. |

## Spring 적용 계약

- Spring MVC 내장 예외와 도메인 예외는 `ProblemDetail`/`ErrorResponse`와
  `ResponseEntityExceptionHandler` 기반의 전역 advice에서 같은 형식으로 변환한다.
- `MethodArgumentNotValidException`, `HandlerMethodValidationException`, query/path type mismatch는
  `VALIDATION_FAILED`로 변환하고 가능한 경우 `fieldErrors`를 채운다.
- JWT 인증 실패는 Spring Security `AuthenticationEntryPoint`, 접근 거부는 `AccessDeniedHandler`에서
  동일한 `ProblemDetail`을 직렬화한다. Security filter 오류가 MVC advice와 다른 JSON을 반환하면 안 된다.
- requestId 생성 filter는 Spring Security chain보다 먼저 적용하고 request attribute와 MDC를 설정한다.
  응답 완료 여부와 관계없이 `finally`에서 MDC를 정리한다.
- `spring.mvc.problemdetails.enabled=true`를 사용하더라도 `code`, `fieldErrors`, `requestId`와 프로젝트
  도메인 code는 별도 중앙 처리가 필요하다.
- 내부 exception class, stack trace, SQL, token, 비밀번호, 정확한 위치는 `detail`이나 `fieldErrors`에
  포함하지 않는다.
- HTTP 응답 status와 body의 `status`는 반드시 같아야 한다.
