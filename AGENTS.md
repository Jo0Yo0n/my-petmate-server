# 핵심 원칙

코드, 설정 또는 테스트를 변경하거나 Git 작업을 하기 전에는 `docs/development-conventions.md`를 읽고 따른다.

기능 구현을 시작하기 전에는 `docs/backend-mvp-plan.md`에서 현재 마일스톤을 확인한다. 이 문서가 상세 마일스톤 문서를 연결한 경우, 해당 문서를 함께 읽는다.

## Learning mode

테스트·Spring·설계 질문에서는 먼저 사용자가
검증 대상, 통제할 의존성, 기대 결과를 판단하게 한다.
답·코드·Mock 설정은 사용자의 시도 또는 명시적 직답 요청 뒤에만 제공한다.

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

코드, 설정 또는 테스트 변경 전에는 `docs/development-conventions.md`를 읽는다. 기능 구현 시에는 현재 마일스톤, 관련 `seed.yaml` 요구사항,
영향을 받는 REST·WebSocket·오류 계약만 확인한다. `docs/m1-auth-contract-checklist.md`는 M1 인증 범위 변경 시에만 적용한다.

제품·API·프로토콜의 동작은 아래 계약 문서 우선순위를 따른다. `docs/development-conventions.md`는 코드 구조, 테스트, Git·PR 절차에 적용한다.

문서가 충돌하면 구현하거나 문서를 수정하지 말고, 기준 문서, 충돌 지점, 권장안과 대안을 사용자에게 보고한다. 사용자의 명시적 승인 없이는 계약 문서를 수정하지 않는다. 적용
우선순위는 다음과 같다.

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
- 사용자가 변경 대상 또는 해결하려는 문제를 명시적으로 요청한 범위의 코드 변경

## AI가 하면 안 되는 일

- 여러 도메인·계층·API 계약을 동시에 바꾸는 작업을, 사용자가 명시한 범위 또는 사전 설계 선택의 확인 없이 구현하는 일
- 현재 요구사항을 만족하는 더 단순한 대안이 있는데도 필요성 설명 없이 새 추상화, 패키지 또는 계층을 도입하는 일
- 보안, 인증·인가, 트랜잭션 경계, 데이터 모델·마이그레이션 또는 영속성 동작의 외부 관찰 가능한 의미를 바꾸는 일을, 영향 범위·위험·대안을 사용자에게 설명하고 승인을 받지
  않은 채 수행하는 일
- 자동으로 커밋하는 일

## 변경 후 설명

Java, Spring, JPA, DB 또는 HTTP에 영향을 주는 변경을 제안하거나 구현한 뒤에는, 실제 변경과 직접 관련된 개념만 다음 형식으로 짧게 설명한다.

- 선택 이유
- 고려한 대안과 선택하지 않은 이유
