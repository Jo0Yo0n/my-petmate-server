# 백엔드 MVP 마일스톤 계획

이 문서는 백엔드의 구현 순서, 현재 진행 상태, 마일스톤별 완료·검증 기준을 관리한다.

- 제품 요구사항과 도메인 개념: [`seed.yaml`](./seed.yaml)
- REST 요청·응답의 상세 계약: [`openapi.yaml`](./openapi.yaml)
- 오류와 요청 ID 계약: [`error-response.md`](./error-response.md)
- WebSocket 계약: [`websocket-protocol.md`](./websocket-protocol.md)
- 문서 전체 위치 안내: [`documentation-guide.md`](./documentation-guide.md)

상세 계약을 이 문서에 복사하지 않는다. 계약이 바뀌면 해당 기준 문서를 먼저 수정하고 이 문서에는 범위와 검증 기준만 반영한다.

## 진행 원칙

- 한 번에 마일스톤 하나만 `진행 중`으로 둔다.
- 이전 마일스톤의 자동 테스트와 짧은 수동 API 확인이 끝난 뒤 다음 마일스톤을 시작한다.
- 일정 날짜가 아니라 완료 조건을 기준으로 상태를 바꾼다.
- 상태 값은 `예정`, `진행 중`, `완료`만 사용한다.

## 진행 현황

| 순서 | 마일스톤            | 상태    | 핵심 결과                                                          |
|------|---------------------|---------|--------------------------------------------------------------------|
| 0    | M0. 백엔드 스캐폴드 | 완료    | 비즈니스 로직 없이 빌드와 기본 의존성을 준비한다.                  |
| 1    | M1. 인증 기반       | 진행 중 | 인증, 토큰 수명 주기, 현재 보호자, 공통 오류와 요청 ID를 완성한다. |
| 2    | M2. 반려견 프로필   | 예정    | 여러 반려견의 프로필과 비공개 사진 저장을 완성한다.                |
| 3    | M3. 추천            | 예정    | 안전 제외 후 요청 시점의 궁합 추천과 선택 필터를 제공한다.         |
| 4    | M4. 좋아요와 매칭   | 예정    | 상호 좋아요로 매칭을 만들고 차단과 구분되는 매칭 종료를 제공한다.  |
| 5    | M5. 채팅            | 예정    | 조회, 읽음 상태, 멱등 WebSocket 전송을 완성한다.                   |
| 6    | M6. 약속과 평가     | 예정    | 약속 상태 전이와 종료 후 비공개 평가를 연결한다.                   |
| 7    | M7. 안전 조치       | 예정    | 차단·신고와 추천·채팅 제외를 완성한다.                             |

현재 프로젝트는 **M1을 진행 중**이다.

## 마일스톤별 계획

### M0. 백엔드 스캐폴드

#### 범위

- Java 21, Spring Boot 3.x, Gradle Kotlin DSL을 유지한다.
- Spotless와 Google Java Format을 설정한다.
- Spring Web, Validation, Security, Data JPA, PostgreSQL, Flyway, WebSocket, 테스트 의존성을 준비한다.
- API와 비즈니스 로직은 구현하지 않는다.

#### 완료 및 검증

- [x] `./gradlew check`가 통과한다.
- [x] 형식 검사가 통과한다.
- [x] 도메인, 컨트롤러, 서비스, 저장소, 마이그레이션 비즈니스 로직이 없다.

### M1. 인증 기반

작업 전 [`m1-auth-contract-checklist.md`](./m1-auth-contract-checklist.md)를 끝까지 읽고 강제 진입 조건을 확인한다.

#### API 범위

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/guardians/me`
- `PATCH /api/guardians/me`

#### 구현 초점

- 보호자와 refresh token 마이그레이션
- 비밀번호 해시와 JWT 발급·검증
- refresh token 회전·폐기·재사용 차단
- 현재 보호자 조회와 유형·공개 설정 수정
- Spring MVC와 Spring Security가 공유하는 REST 오류 형식
- 모든 HTTP 요청에 적용되는 서버 생성 요청 ID

세부 필드와 토큰 정책은 `openapi.yaml`, 오류와 요청 ID는 `error-response.md`가 기준이다.

#### 완료 기준

- [ ] 회원가입, 로그인, 토큰 갱신, 로그아웃이 계약대로 동작한다.
- [ ] 유효한 access token으로 현재 보호자와 계정 상태를 복원한다.
- [ ] 개인·커플·가족 유형과 공개 설정의 조건부 검증이 동작한다.
- [ ] 보호된 API가 access token 누락·오류 요청을 거부한다.
- [ ] validation, 인증, 권한, 리소스 없음, 충돌 오류가 공통 `ProblemDetail` 계약과 일치한다.
- [ ] [`m1-auth-contract-checklist.md`](./m1-auth-contract-checklist.md)의 모든 검증·완료 게이트를 통과한다.

#### 핵심 검증

- token 생성·검증·회전·폐기 단위 테스트
- 인증 API와 현재 보호자 API의 정상·실패 통합 테스트
- 보호자 유형별 조건부 validation 테스트
- MVC 4xx와 Security 401·403 오류 계약 테스트
- 정상·오류 응답의 요청 ID 형식·유일성·전달·정리 테스트

### M2. 반려견 프로필

#### API 범위

- `POST /api/dogs`
- `GET /api/dogs`
- `GET /api/dogs/{dogId}`
- `PATCH /api/dogs/{dogId}`
- `POST /api/dogs/{dogId}/photos`

#### 구현 초점

- 반려견과 반복 가능한 산책 시간대 마이그레이션
- 한 보호자가 여러 반려견을 관리하는 소유권 모델
- 프로필 JSON과 초기 사진을 받는 multipart 생성
- AWS S3 비공개 객체 저장과 만료되는 조회 URL 반환
- 사진 형식·크기·해상도 검증과 실패 시 객체 정리

#### 완료 기준

- [ ] 소유한 반려견의 생성·목록·조회·수정이 동작한다.
- [ ] 생성 시 초기 사진을 올리고 이후 사진을 교체할 수 있다.
- [ ] 다른 보호자의 반려견을 조회하거나 수정할 수 없다.
- [ ] 견종, 나이, 안전·궁합 필드가 `openapi.yaml` 계약대로 검증된다.

#### 핵심 검증

- 생성·목록·조회·수정 API 테스트
- 사진 업로드·교체의 정상·실패·권한 테스트
- DB 저장 실패 시 업로드 객체 정리 테스트
- 필수 필드, enum, 소유권 테스트

### M3. 추천

#### API 범위

- `GET /api/dogs/{dogId}/recommendations`

query parameter의 이름과 형식은 `openapi.yaml`의 해당 경로를 기준으로 한다.

#### 구현 초점

- 선택한 반려견을 기준으로 요청 시점에 추천 계산
- `안전·차단·신고 제외 → 공개 상호성 → 선택한 선호 조건 → 궁합 정렬` 순서 보장
- 공개 보호자만 사용할 수 있는 보호자 정보 필터와 비공개 정보 마스킹
- 정확한 위치 대신 근사 거리, 숫자 점수 대신 등급과 사람이 읽을 수 있는 이유 반환
- M3에서는 사용 가능한 안전 조치 데이터를 추천에서 제외하고, 차단·신고 API와 전체 연동은 M7에서 완성

#### 완료 기준

- [ ] 위험하거나 명확히 맞지 않는 후보가 정렬 전에 제외된다.
- [ ] 생략한 필터 차원은 후보를 제한하지 않는다.
- [ ] 선호 조건이 안전 제외를 완화하지 못한다.
- [ ] 공개 상호성과 비공개 마스킹이 제품 요구사항대로 동작한다.
- [ ] 정확한 위치와 비공개 보호자 정보가 응답에 포함되지 않는다.

#### 핵심 검증

- 안전 제외와 궁합 계산 단위 테스트
- 단일·복합 query와 잘못된 값의 validation 테스트
- 공개 상호성, 정보 필터, 비공개 마스킹 테스트
- 안전 규칙 우회 방지와 위치·신원 privacy 테스트

### M4. 좋아요와 매칭

#### API 범위

- `POST /api/dogs/{dogId}/likes`
- `GET /api/matches`
- `GET /api/matches/{matchId}`
- `POST /api/matches/{matchId}/close`

#### 구현 초점

- 반려견 쌍의 `liked`·`passed` 상호작용과 방향별 중복 방지
- 상호 좋아요가 된 반려견 쌍에 활성 매칭 하나만 생성
- 참여자만 수행할 수 있는 멱등 매칭 종료
- 종료를 차단·신고와 구분하고 종료된 매칭의 채팅 사용 금지

#### 완료 기준

- [ ] 한쪽 좋아요만으로 매칭이 생성되지 않는다.
- [ ] 상호 좋아요 시 활성 매칭이 정확히 하나 생성된다.
- [ ] 참여자는 활성 매칭을 종료할 수 있고 비참여자는 종료할 수 없다.

#### 핵심 검증

- 상호 좋아요와 동시성·고유 제약 테스트
- 좋아요·패스 API 테스트
- 매칭 종료의 정상·멱등·권한 테스트

### M5. 채팅

#### API 범위

- `GET /api/matches/{matchId}/messages`
- `POST /api/matches/{matchId}/read`
- `GET /api/matches` 응답의 최근 메시지와 읽지 않은 수
- `WS /ws`

#### 구현 초점

- cursor 기반 이전 메시지 역방향 조회
- 보호자별 마지막 읽은 메시지와 읽지 않은 수
- WebSocket 연결 인증과 명시적인 성공·실패 event
- `(match_id, sender_guardian_id, client_message_id)` 범위의 전송 멱등성
- 참여자 권한과 종료·차단된 매칭 접근 차단
- 약속 카드의 상태 전이와 평가 가능 상태는 M6에서 구현

프레임과 재연결의 상세 형식은 [`websocket-protocol.md`](./websocket-protocol.md)가 기준이다.

#### 완료 기준

- [ ] 참여 보호자가 텍스트 메시지를 주고받을 수 있다.
- [ ] 이전 메시지 조회와 재연결 후 전송 복구가 중복 없이 동작한다.
- [ ] 매칭 목록에서 최근 메시지와 읽지 않은 수를 확인하고 읽음 위치를 갱신한다.
- [ ] 비참여자와 종료·차단된 매칭은 조회·읽음·전송이 거부된다.

#### 핵심 검증

- cursor 경계·limit·정렬·권한 API 테스트
- 읽음 위치와 참여자별 읽지 않은 수 격리 테스트
- 인증·성공·실패 WebSocket 통합 테스트
- 같은 `clientMessageId` 재전송과 충돌 테스트

### M6. 약속과 평가

#### API 범위

- `POST /api/matches/{matchId}/appointment-proposals`
- `POST /api/appointment-proposals/{proposalId}/accept`
- `POST /api/appointment-proposals/{proposalId}/reject`
- `POST /api/appointment-proposals/{proposalId}/cancel`
- `POST /api/appointment-proposals/{proposalId}/review`

#### 구현 초점

- `Asia/Seoul` 기준 약속 제안·수락·거절·취소 상태 전이
- 수락된 약속의 종료 시각 이후 참여자별 한 번만 가능한 상대 보호자 평가
- 평가 원문과 집계 점수 비공개
- 약속 카드의 평가 가능·제출 상태 파생

#### 완료 기준

- [ ] 상대 보호자가 약속을 수락하거나 거절할 수 있다.
- [ ] 수락된 약속을 사유와 함께 취소할 수 있다.
- [ ] 각 참여자는 종료된 수락 약속에서 상대 보호자를 한 번 평가할 수 있다.
- [ ] 부적격 약속, 비참여자, 자기 자신, 중복 평가는 거부된다.

#### 핵심 검증

- 상태 전이와 약속 수명 주기 API 테스트
- 시간·점수·태그 validation 테스트
- 평가 고유 제약·권한·privacy 테스트

### M7. 안전 조치

#### API 범위

- `POST /api/guardians/{guardianId}/block`
- `POST /api/guardians/{guardianId}/reports`

#### 구현 초점

- 차단과 허용된 사유를 사용하는 신고
- 차단·신고 대상의 추천과 관련 채팅 상호작용 제외
- 누적 신고 또는 서버가 분류한 고위험 신고에 따른 임시 접근 제한
- 사용자 입력에서 신고 심각도를 받지 않음

#### 완료 기준

- [ ] 차단이 이후 추천과 관련 채팅 상호작용을 막는다.
- [ ] 신고 사유와 선택 설명이 계약대로 검증된다.
- [ ] 누적 또는 고위험 신고가 매칭·채팅 임시 제한으로 이어질 수 있다.

#### 핵심 검증

- 차단·신고 API와 멱등성 테스트
- 신고 사유·설명·심각도 입력 금지 validation 테스트
- 추천 제외와 차단 이후 채팅 접근 테스트

## 상태 변경 절차

1. 현재 마일스톤의 완료 기준과 핵심 검증을 모두 확인한다.
2. `./gradlew check`와 필요한 수동 API 확인을 수행한다.
3. 보안·인증·데이터베이스·트랜잭션 변경은 사람 개발자의 검토 결과를 기록한다.
4. 현재 마일스톤을 `완료`로 바꾼다.
5. 다음 마일스톤 하나만 `진행 중`으로 바꾼다.
