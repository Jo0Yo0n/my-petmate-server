# 문서 안내서

이 문서는 `docs` 디렉터리의 시작점이다. 같은 내용을 여러 문서에 반복하지 않고, 아래 표에 지정한 기준 문서에서만 상세 내용을 관리한다.

## 빠른 찾기

| 확인할 내용                               | 기준 문서                                                          | 위치                                                                                                                                                                                           |
|-------------------------------------------|--------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 프로젝트 목표와 MVP 범위                  | [`seed.yaml`](./seed.yaml)                                         | `goal`, `constraints`                                                                                                                                                                          |
| 사용자 관점의 완료 조건                   | [`seed.yaml`](./seed.yaml)                                         | `acceptance_criteria`                                                                                                                                                                          |
| 도메인 개념과 주요 필드                   | [`seed.yaml`](./seed.yaml)                                         | `ontology_schema`                                                                                                                                                                              |
| 평가 원칙, 지표, MVP 종료 조건            | [`seed.yaml`](./seed.yaml)                                         | `evaluation_principles`, `success_metrics`, `exit_conditions`                                                                                                                                  |
| 현재 마일스톤과 구현 순서                 | [`backend-mvp-plan.md`](./backend-mvp-plan.md)                     | [진행 현황](./backend-mvp-plan.md#진행-현황), [마일스톤별 계획](./backend-mvp-plan.md#마일스톤별-계획)                                                                                         |
| 마일스톤별 API 범위·작업·완료·테스트 기준 | [`backend-mvp-plan.md`](./backend-mvp-plan.md)                     | `M0`~`M7` 절                                                                                                                                                                                   |
| HTTP 메서드·경로·요청·응답 스키마         | [`openapi.yaml`](./openapi.yaml)                                   | `paths`, `components`                                                                                                                                                                          |
| 토큰 수명과 개인정보 정책                 | [`openapi.yaml`](./openapi.yaml)                                   | `info.x-token-policy`, `info.x-privacy-policy`                                                                                                                                                 |
| REST 오류 본문·오류 코드·요청 ID          | [`error-response.md`](./error-response.md)                         | [응답 구조](./error-response.md#응답-구조), [오류 코드](./error-response.md#오류-코드), [요청-id](./error-response.md#요청-id)                                                                 |
| WebSocket 인증·이벤트·멱등성·재연결       | [`websocket-protocol.md`](./websocket-protocol.md)                 | [연결과 인증](./websocket-protocol.md#연결과-인증), [이벤트](./websocket-protocol.md#이벤트), [멱등성과 권한](./websocket-protocol.md#멱등성과-권한), [재연결](./websocket-protocol.md#재연결) |
| M1 인증 구현 순서와 완료 점검             | [`m1-auth-contract-checklist.md`](./m1-auth-contract-checklist.md) | [진입 조건](./m1-auth-contract-checklist.md#진입-조건), [구현 순서](./m1-auth-contract-checklist.md#구현-순서), [검증 게이트](./m1-auth-contract-checklist.md#검증-게이트)                     |
| 코드·브랜치·커밋·PR 규칙                  | [`development-conventions.md`](./development-conventions.md)       | [코드와 설계](./development-conventions.md#코드와-설계), [Git 작업](./development-conventions.md#git-작업), [Pull Request](./development-conventions.md#pull-request)                          |

## 문서별 역할

| 문서                            | 이 문서에 기록하는 내용                          | 이 문서에 기록하지 않는 내용        |
|---------------------------------|--------------------------------------------------|-------------------------------------|
| `seed.yaml`                     | 제품 목표, 제약, 인수 기준, 도메인 개념          | 상세 HTTP·WebSocket 형식, 구현 일정 |
| `backend-mvp-plan.md`           | 현재 진행 상태, 마일스톤별 구현·완료·테스트 범위 | 요청·응답 필드의 상세 정의          |
| `openapi.yaml`                  | REST API의 실제 통신 계약                        | 제품 배경, 구현 작업 목록           |
| `error-response.md`             | 모든 REST 오류와 요청 ID의 공통 계약             | 개별 성공 응답                      |
| `websocket-protocol.md`         | WebSocket 연결과 프레임 계약                     | REST 채팅 조회 계약                 |
| `m1-auth-contract-checklist.md` | M1 작업 진입·검증·완료 여부                      | 계약 상세 설명의 재작성             |
| `development-conventions.md`    | 저장소 작업 방식과 검증 규칙                     | 기능 요구사항                       |

## 기능별 읽기 순서

### 일반 기능 개발

1. `development-conventions.md`에서 작업 규칙을 확인한다.
2. `backend-mvp-plan.md`의 진행 현황과 해당 마일스톤을 확인한다.
3. `seed.yaml`의 관련 인수 기준과 도메인 개념을 확인한다.
4. REST 작업은 `openapi.yaml`, WebSocket 작업은 `websocket-protocol.md`를 확인한다.
5. 오류를 추가하거나 변경하면 `error-response.md`를 함께 확인한다.

### M1 인증·JWT·토큰·요청 ID·오류 처리

1. `m1-auth-contract-checklist.md`를 처음부터 끝까지 읽는다.
2. 체크리스트가 연결하는 `openapi.yaml`과 `error-response.md` 계약을 확인한다.
3. 사람 개발자의 설계 검토를 받은 뒤 구현한다.
4. 체크리스트의 검증 게이트를 모두 통과한 뒤에만 M1 상태를 완료로 바꾼다.

## 충돌 처리

문서끼리 내용이 다르면 코드에서 임의로 해석하지 말고 문서를 먼저 일치시킨다. 계약 종류별 우선 기준은 다음과 같다.

1. REST 통신 형식: `openapi.yaml`
2. REST 오류와 요청 ID: `error-response.md`
3. WebSocket 통신 형식: `websocket-protocol.md`
4. 제품 범위와 인수 기준: `seed.yaml`
5. 구현 순서와 마일스톤 완료 조건: `backend-mvp-plan.md`
6. M1 작업의 진입·검증 절차: `m1-auth-contract-checklist.md`
7. 저장소 작업 방식: `development-conventions.md`

우선순위가 낮은 문서는 상위 기준 문서의 상세 계약을 복사하지 않고 링크만 유지한다.
