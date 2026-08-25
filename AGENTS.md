# 핵심 원칙

프로덕션 코드의 대부분은 사람 개발자가 작성해야 한다.

코드·빌드 설정을 변경하거나 커밋하기 전에는 `docs/development-conventions.md`를 읽고 따른다.

마일스톤 작업을 시작하기 전에는 `docs/backend-mvp-plan.md`에서 현재 마일스톤을 확인한다.

M1 인증, JWT, refresh token, 요청 ID, 오류 처리를 작업하기 전에는
`docs/m1-auth-contract-checklist.md`와 그 문서가 연결하는 OpenAPI·오류 응답 문서를 모두 끝까지 읽는다. 필수 체크 항목과 검증 게이트를 모두
충족하기 전에는 M1을 완료로 표시하지 않는다.

## 문서별 기준 정보

상세 정보는 다른 문서에 복사하지 않고, 아래에서 지정한 기준 문서에만 관리한다.

| 주제                                           | 기준 문서                            |
|------------------------------------------------|--------------------------------------|
| 제품 목표, MVP 범위, 인수 기준, 도메인 개념    | `docs/seed.yaml`                     |
| 현재 마일스톤, 구현 순서, 완료 기준            | `docs/backend-mvp-plan.md`           |
| REST 경로, 요청·응답, 토큰 정책, 개인정보 정책 | `docs/openapi.yaml`                  |
| REST 오류 응답과 요청 ID 계약                  | `docs/error-response.md`             |
| WebSocket 연결, 이벤트, 멱등성, 재연결         | `docs/websocket-protocol.md`         |
| M1 인증 진입 조건과 검증 게이트                | `docs/m1-auth-contract-checklist.md` |
| 코드, Git, Pull Request 규칙                   | `docs/development-conventions.md`    |

일반 기능 작업에서는 개발 규칙을 읽고, 현재 마일스톤과 관련된 `seed.yaml` 요구사항을 확인한 뒤, 해당하는 REST 또는 WebSocket 계약과 오류 계약을 확인한다.

M1 인증, JWT, refresh token, 현재 보호자, 요청 ID 또는 오류 처리 작업은 다음 순서를 따른다.

1. `docs/m1-auth-contract-checklist.md`를 끝까지 읽는다.
2. 이 문서가 연결하는 `docs/openapi.yaml`, `docs/error-response.md` 계약을 읽는다.
3. 구현 전에 필요한 사람 개발자의 설계 검토를 받는다.
4. 모든 검증 게이트를 통과한 뒤에만 M1을 완료로 표시한다.

문서가 충돌하면 구현 방식을 선택하기 전에 문서부터 일치시킨다. 적용 우선순위는 다음과 같다.

1. REST 통신 형식: `docs/openapi.yaml`
2. REST 오류와 요청 ID: `docs/error-response.md`
3. WebSocket 프로토콜: `docs/websocket-protocol.md`
4. 제품 범위와 인수 기준: `docs/seed.yaml`
5. 마일스톤 순서와 완료 기준: `docs/backend-mvp-plan.md`
6. M1 진입·검증 절차: `docs/m1-auth-contract-checklist.md`
7. 저장소 작업 규칙: `docs/development-conventions.md`

## AI가 할 수 있는 일

- 명확화 질문
- 아키텍처 대안 제안
- 구현 계획 초안 작성
- 변경 사항 검토
- 테스트 제안
- 오류 설명
- 명시적으로 요청받은 작은 독립 코드 조각 생성

## AI가 하면 안 되는 일

- 승인 없이 광범위한 기능을 처음부터 끝까지 구현하는 일
- 필요성 증명 없이 새 추상화를 도입하는 일
- 이유를 설명하지 않고 새 패키지나 계층을 만드는 일
- 명시적인 검토 없이 보안, 트랜잭션, 영속성 동작을 변경하는 일
- 자동으로 커밋하는 일
