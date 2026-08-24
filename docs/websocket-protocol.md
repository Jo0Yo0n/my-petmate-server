# WebSocket 프로토콜

이 문서는 실시간 채팅의 연결, 인증, 프레임 이벤트, 권한, 멱등성, 재연결 규칙을 정하는 기준 문서다. REST 메시지 조회·읽음 API는 [`openapi.yaml`](./openapi.yaml), 공통 오류 코드의 의미는 [`error-response.md`](./error-response.md#오류-코드)에서 확인한다.

## 연결과 인증

- endpoint는 `/ws`이며 JSON 텍스트 프레임만 사용한다.
- 클라이언트는 연결 시 HTTP `Authorization: Bearer {accessToken}` 헤더로 인증한다.
- 서버는 JWT 검증이 끝나면 아래 `auth_ok` 이벤트를 보낸다. 클라이언트는 이 이벤트 전에는 메시지를 보내지 않는다.
- token 누락·오류·만료 시 서버는 `message_error`를 보낸 뒤 연결을 닫는다.

```json
{
  "type": "auth_ok",
  "guardianId": "1c29bceb-6e56-4c55-a62d-f0b6a0612b59",
  "connectedAt": "2026-07-23T11:00:00+09:00"
}
```

## 이벤트

### 텍스트 메시지 전송

클라이언트 프레임은 다음 세 필드를 정확히 포함한다. `body`는 공백만으로 구성할 수 없고 UTF-8 기준 1~2000자다.

```json
{
  "matchId": "45dd6b51-5924-4b2d-aab8-697f09a12bbb",
  "clientMessageId": "641127ff-07b4-4868-98b0-eb9a75d9d73c",
  "body": "토요일 오전은 어떠세요?"
}
```

저장 성공 시 보낸 연결과 상대 참여자의 활성 연결 모두에 `message_created`를 보낸다.

```json
{
  "type": "message_created",
  "message": {
    "id": "6ffb927d-7958-493f-92e7-d14bcd30edbc",
    "matchId": "45dd6b51-5924-4b2d-aab8-697f09a12bbb",
    "senderGuardianId": "1c29bceb-6e56-4c55-a62d-f0b6a0612b59",
    "clientMessageId": "641127ff-07b4-4868-98b0-eb9a75d9d73c",
    "messageType": "text",
    "body": "토요일 오전은 어떠세요?",
    "appointmentProposal": null,
    "createdAt": "2026-07-23T11:02:00+09:00"
  }
}
```

### 전송 실패

실패 시 요청을 연관 지을 수 있도록 가능한 경우 `matchId`와 `clientMessageId`를 그대로 반환한다.
WebSocket 이벤트는 HTTP 응답이 아니므로 RFC 9457 `ProblemDetail` 전체를 사용하지 않는다. 다만 REST와 같은
`code`, `detail`, 선택적 `fieldErrors`, `requestId` 의미를 유지한다.

서버는 HTTP handshake의 `requestId`를 프레임 처리에 재사용하지 않는다. 수신한 클라이언트 프레임을 처리할
때마다 REST와 같은 `req-` + 26자 Crockford Base32 ULID 형식으로 새 requestId를 생성하고 MDC key
`requestId`에 저장한다. 실패 시 `message_error`와 서버 로그에 같은 값을 사용하며 프레임 처리가 끝나면
`finally`에서 MDC를 제거한다. 이 값은 로그 상관관계용이며 메시지 멱등성은 아래
`clientMessageId` 계약이 담당한다.

```json
{
  "type": "message_error",
  "matchId": "45dd6b51-5924-4b2d-aab8-697f09a12bbb",
  "clientMessageId": "641127ff-07b4-4868-98b0-eb9a75d9d73c",
  "error": {
    "code": "MATCH_CLOSED",
    "detail": "종료된 매칭에서는 메시지를 보낼 수 없습니다.",
    "requestId": "req-01ARZ3NDEKTSV4RRFFQ69G5FAV"
  }
}
```

허용하는 주요 오류 `code`는 `AUTH_TOKEN_EXPIRED`, `AUTH_TOKEN_INVALID`, `VALIDATION_FAILED`, `FORBIDDEN`, `RESOURCE_NOT_FOUND`, `MATCH_CLOSED`, `MATCH_BLOCKED`, `ACCOUNT_RESTRICTED`, `MESSAGE_IDEMPOTENCY_CONFLICT`, `MESSAGE_SEND_FAILED`다.

## 멱등성과 권한

- active match의 두 참여 보호자만 전송할 수 있다.
- closed 또는 blocked match의 전송은 각각 `MATCH_CLOSED`, `MATCH_BLOCKED`로 거부한다.
- 멱등성 범위는 `(matchId, senderGuardianId, clientMessageId)`다.
- 동일 범위의 동일한 `clientMessageId`를 재전송하면 새 메시지를 만들지 않고 최초 저장된 메시지와 동일한 `message_created`를 반환한다.
- 동일 범위의 `clientMessageId`에 서로 다른 `body`를 보내면 `MESSAGE_IDEMPOTENCY_CONFLICT`를 반환한다.
- 서로 다른 match 또는 sender의 동일 `clientMessageId`는 별도 요청이다.

## 재연결

1. 연결이 끊기거나 access token이 만료되면 클라이언트는 진행 중인 자동 재연결을 하나로 합친다.
2. access token 만료가 원인이면 REST refresh를 한 번 실행해 access/refresh token을 회전한다.
3. 새 access token으로 `/ws`에 다시 연결하고 `auth_ok`를 기다린다.
4. 각 열린 match에서 `GET /api/matches/{matchId}/messages`를 다시 조회해 연결 공백 동안의 메시지를 복구한다.
5. `message_created`를 받지 못한 전송은 원래 `clientMessageId`로 재전송한다. 서버의 멱등성 보장 때문에 중복 저장되지 않는다.
6. 클라이언트는 오프라인 메시지 큐를 영구 저장하지 않는다. 재연결 중 사용자가 명시적으로 재시도한 현재 세션 전송만 복구한다.

약속 제안과 상태 변경은 REST API로 수행한다. 그 결과 생성되는 `appointment_proposal` 메시지 카드는 REST 메시지 내역 재조회로 동기화하며, 이 MVP WebSocket 클라이언트 전송 계약은 텍스트 메시지만 다룬다.
