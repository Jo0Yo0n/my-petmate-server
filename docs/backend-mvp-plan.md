# 백엔드 MVP 계획

이 문서는 `docs/seed.yaml`을 사람이 한 마일스톤씩 직접 구현할 수 있을 만큼 작은 백엔드 전용 작업으로 나눈다. 이 문서는 의도적으로 계획 문서이며, 비즈니스 코드가 아니다.

## 일정 기준

- 기준일: 2026-07-13.
- 각 마일스톤은 이전 마일스톤의 테스트와 짧은 수동 API 확인이 끝난 뒤 시작한다.
- 날짜는 백엔드 MVP 계획 기준의 고정 목표일이며, 범위가 바뀌면 이 표를 먼저 갱신한다.
- M0는 기준일부터 3영업일로 잡고, M1부터는 월요일에 시작하는 5영업일 단위로 진행한다.

| 마일스톤 | 기간 | 상태 | 범위 판단 |
| --- | --- | --- | --- |
| M0. 백엔드 스캐폴드 | 2026-07-13 ~ 2026-07-15 | 완료 | 비즈니스 로직 없이 빌드와 기본 의존성까지만 둔다. |
| M1. 인증 기반 | 2026-07-20 ~ 2026-07-24 | 예정 | JWT, refresh token, 보호 endpoint까지 한 묶음으로 구현해야 이후 API 권한 테스트가 가능하다. |
| M2. 반려견 프로필 | 2026-07-27 ~ 2026-07-31 | 예정 | 추천, like, match의 입력 데이터가 되므로 M3 전에 끝낸다. |
| M3. 추천 | 2026-08-03 ~ 2026-08-07 | 예정 | 실시간 배치 없이 요청 시점 계산만 구현한다. 차단/신고 제외는 최소 훅만 반영하고 M7에서 완성한다. |
| M4. Like와 Match | 2026-08-10 ~ 2026-08-14 | 예정 | 상호 like와 match 생성까지만 다루고 채팅은 포함하지 않는다. |
| M5. 채팅 | 2026-08-17 ~ 2026-08-21 | 예정 | WebSocket과 권한 검증까지만 포함하고 appointment card의 상태 처리는 M6로 넘긴다. |
| M6. 약속 제안 | 2026-08-24 ~ 2026-08-28 | 예정 | 채팅 안의 약속 카드 메시지와 제안 상태 전이를 연결한다. |
| M7. 안전 조치 | 2026-08-31 ~ 2026-09-04 | 예정 | 차단/신고 API와 추천/채팅 제외를 완성하되, 관리자 검토 화면은 MVP 밖으로 둔다. |

## 현재 위치

현재 프로젝트는 M0를 완료했으며 M1 시작 전이다.

AI agent가 현재 마일스톤을 알아야 할 때는 이 섹션과 위 일정 표를 먼저 읽는다. 상태 값은 `진행 중`, `예정`, `완료`만 사용한다. 다음 마일스톤을 시작하기 전에 이전 마일스톤 상태를 `완료`로 바꾸고, 새 마일스톤 하나만 `진행 중`으로 둔다.

## 도메인 모델 초안

| 모델 | 목적 | 주요 필드 |
| --- | --- | --- |
| Guardian | 한 마리 이상의 반려견을 등록하는 계정 소유자 | id, email, password_hash, gender, status, created_at, updated_at |
| RefreshToken | JWT refresh token 수명 주기 관리 | id, guardian_id, token_hash, expires_at, revoked_at, created_at |
| Dog | 매칭에 사용하는 반려견 프로필 | id, guardian_id, name, photo_url, breed, is_mixed_breed, birth_month_or_age, sex, weight_kg, size_group, neutered, primary_activity_area, approximate_latitude, approximate_longitude, vaccinated, animal_registered, sociability, aggression_level, preferred_walk_intensity, unfamiliar_dog_reaction, self_introduction |
| DogAvailableTimeSlot | 반려견의 반복 가능한 산책 가능 시간대 | id, dog_id, day_of_week, start_time, end_time |
| DogInteraction | 반려견 쌍 사이의 like 또는 pass | id, from_dog_id, to_dog_id, status, created_at |
| Match | 두 반려견의 상호 like로 생성되는 매칭 | id, dog_a_id, dog_b_id, status, created_at, closed_at |
| ChatMessage | 매칭 채팅방의 텍스트 또는 약속 카드 메시지 | id, match_id, sender_guardian_id, message_type, body, appointment_proposal_id, created_at |
| AppointmentProposal | 산책/놀이 약속 제안 | id, match_id, proposed_by_guardian_id, date, time_slot, place_text, status, cancellation_reason, created_at, decided_at |
| SafetyAction | 차단 또는 신고 조치 | id, reporter_guardian_id, target_guardian_id, type, reason, severity, created_at |

## API 초안

| 영역 | Method and Path | 목적 |
| --- | --- | --- |
| Auth | `POST /api/auth/signup` | 보호자 계정 생성 |
| Auth | `POST /api/auth/login` | access token과 refresh token 발급 |
| Auth | `POST /api/auth/refresh` | access token 갱신 |
| Auth | `POST /api/auth/logout` | refresh token 폐기 |
| Dogs | `POST /api/dogs` | 반려견 프로필 생성 |
| Dogs | `GET /api/dogs` | 내 반려견 목록 조회 |
| Dogs | `GET /api/dogs/{dogId}` | 내 반려견 프로필 조회 |
| Dogs | `PATCH /api/dogs/{dogId}` | 내 반려견 프로필 수정 |
| Recommendations | `GET /api/dogs/{dogId}/recommendations` | 요청 시점에 추천 목록 계산 |
| Likes | `POST /api/dogs/{dogId}/likes` | 추천 반려견에 like 또는 pass |
| Matches | `GET /api/matches` | 활성 매칭 목록 조회 |
| Matches | `GET /api/matches/{matchId}` | 매칭 상세 조회 |
| Chat | `GET /api/matches/{matchId}/messages` | 채팅 내역 조회 |
| Chat | `WS /ws` | 매칭 채팅 메시지 교환 |
| Appointments | `POST /api/matches/{matchId}/appointment-proposals` | 약속 제안 |
| Appointments | `POST /api/appointment-proposals/{proposalId}/accept` | 약속 제안 수락 |
| Appointments | `POST /api/appointment-proposals/{proposalId}/reject` | 약속 제안 거절 |
| Appointments | `POST /api/appointment-proposals/{proposalId}/cancel` | 수락된 약속 취소 |
| Safety | `POST /api/guardians/{guardianId}/block` | 보호자 차단 |
| Safety | `POST /api/guardians/{guardianId}/reports` | 보호자 신고 |

## DB 테이블 초안

| 테이블 | 비고 |
| --- | --- |
| guardians | 고유 email, password hash, gender, status 저장 |
| refresh_tokens | token hash만 저장하고 guardian과 연결, 폐기 시각 저장 |
| dogs | 한 guardian이 여러 dog를 소유한다. 정확한 실시간 위치가 아니라 활동 지역과 근사 좌표만 저장한다. |
| dog_available_time_slots | 한 dog가 여러 가능 시간대를 가진다. |
| dog_interactions | `(from_dog_id, to_dog_id)` 고유. status는 liked 또는 passed. |
| matches | 순서 없는 dog 쌍 기준 고유. active, blocked, closed 상태를 가진다. |
| chat_messages | match와 메시지를 보낸 guardian에 연결된다. |
| appointment_proposals | match에 연결된다. 상태 흐름은 proposed, accepted, rejected, canceled. |
| safety_actions | guardian 사이의 차단과 신고를 기록한다. |

## 마일스톤

### M0. 백엔드 스캐폴드

구현 API:

- 없음. API 구현은 M1부터 시작한다.

작업:

- Java 21, Spring Boot 3.x, Gradle Kotlin DSL을 유지한다.
- Spotless와 Google Java Format을 설정한다.
- Spring Web, Validation, Security, Data JPA, PostgreSQL, Flyway, WebSocket, test 의존성을 추가한다.

완료 기준:

- `./gradlew check`가 통과한다.
- domain, controller, service, repository, migration 비즈니스 로직이 아직 없어야 한다.

테스트 기준:

- Gradle check.
- Spotless check.

### M1. 인증 기반

구현 API:

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

작업:

- guardian과 refresh token migration을 정의한다.
- signup, login, refresh, logout을 구현한다.
- 비밀번호는 hash로 저장하고 refresh token은 hash만 저장한다.

완료 기준:

- 보호자가 회원가입, 로그인, 토큰 갱신, 로그아웃을 할 수 있다.
- 보호된 테스트 endpoint가 access token 누락/오류 요청을 거부한다.

테스트 기준:

- token 생성과 refresh 검증 단위 테스트.
- signup/login/refresh/logout 정상 흐름과 잘못된 인증 정보 흐름 API 테스트.

### M2. 반려견 프로필

구현 API:

- `POST /api/dogs`
- `GET /api/dogs`
- `GET /api/dogs/{dogId}`
- `PATCH /api/dogs/{dogId}`

작업:

- dog와 availability migration을 정의한다.
- 반려견 프로필 생성, 목록 조회, 단건 조회, 수정 API를 구현한다.
- 필수 안전/궁합 필드를 검증한다.

완료 기준:

- 한 보호자가 여러 반려견을 관리할 수 있다.
- 보호자가 다른 보호자의 반려견을 조회하거나 수정할 수 없다.

테스트 기준:

- create/list/read/edit API 테스트.
- 필수 필드 누락과 잘못된 enum 값 검증 테스트.
- 다른 소유자의 데이터 접근 차단 테스트.

### M3. 추천

구현 API:

- `GET /api/dogs/{dogId}/recommendations`

작업:

- 선택된 한 반려견 기준으로 요청 시점 추천 계산을 구현한다.
- 위험한 조합과 차단/신고된 보호자를 제외한다.
- 숫자 퍼센트 점수가 아니라 compatibility grade와 사람이 읽을 수 있는 이유를 반환한다.

완료 기준:

- 위험하거나 명확히 맞지 않는 후보는 정렬 전에 제외된다.
- 결과에는 근사 거리와 추천 이유만 표시된다.

테스트 기준:

- 제외 규칙 단위 테스트.
- 추천 목록과 상세 응답 형태 API 테스트.
- 정확한 실시간 위치가 반환되지 않음을 증명하는 privacy 테스트.

### M4. Like와 Match

구현 API:

- `POST /api/dogs/{dogId}/likes`
- `GET /api/matches`
- `GET /api/matches/{matchId}`

작업:

- 반려견 쌍에 대한 like/pass를 구현한다.
- 양쪽이 모두 like한 뒤에만 match를 생성한다.
- 같은 방향의 반려견 쌍 interaction 중복을 막는다.

완료 기준:

- 한쪽만 like한 상태에서는 match가 생성되지 않는다.
- 상호 like가 발생하면 active match가 정확히 하나 생성된다.

테스트 기준:

- 상호 like에 따른 match 생성 service 테스트.
- like/pass API 테스트.
- 중복 interaction 처리 constraint 테스트.

### M5. 채팅

구현 API:

- `GET /api/matches/{matchId}/messages`
- `WS /ws`

작업:

- match 범위의 메시지 내역을 추가한다.
- WebSocket 텍스트 메시지 흐름을 추가한다.
- 매칭된 보호자만 메시지를 읽거나 보낼 수 있게 제한한다.

완료 기준:

- 매칭된 보호자가 텍스트 메시지를 주고받을 수 있다.
- 참여자가 아닌 사용자는 match chat에 접근할 수 없다.

테스트 기준:

- 메시지 내역 권한 API 테스트.
- 텍스트 메시지 정상 흐름 WebSocket integration 테스트.

### M6. 약속 제안

구현 API:

- `POST /api/matches/{matchId}/appointment-proposals`
- `POST /api/appointment-proposals/{proposalId}/accept`
- `POST /api/appointment-proposals/{proposalId}/reject`
- `POST /api/appointment-proposals/{proposalId}/cancel`

작업:

- 약속 제안, 수락, 거절, 취소 흐름을 구현한다.
- 수락된 약속을 취소할 때 cancellation reason을 저장한다.

완료 기준:

- 상대 보호자가 약속 제안을 수락하거나 거절할 수 있다.
- 수락된 약속은 사유와 함께 취소할 수 있다.

테스트 기준:

- 상태 전이 테스트.
- 약속 제안 수명 주기 API 테스트.
- 참여자가 아닌 사용자의 접근 차단 테스트.

### M7. 안전 조치

구현 API:

- `POST /api/guardians/{guardianId}/block`
- `POST /api/guardians/{guardianId}/reports`

작업:

- 차단과 신고 API를 구현한다.
- 차단/신고된 보호자를 추천과 채팅 상호작용에서 제외한다.
- 심각하거나 누적된 신고에 대해 matching/chat 접근을 임시 제한한다.

완료 기준:

- 차단은 이후 추천과 채팅 상호작용을 막는다.
- 심각한 신고는 matching/chat 접근 제한으로 이어질 수 있다.

테스트 기준:

- block/report API 테스트.
- 추천 제외 테스트.
- 차단 이후 채팅 접근 테스트.

## 구현 순서

1. M0 scaffold
2. M1 auth foundation
3. M2 dog profiles
4. M3 recommendations
5. M4 likes and matches
6. M5 chat
7. M6 appointments
8. M7 safety actions

이전 마일스톤의 테스트가 통과하고 짧은 수동 API 확인이 끝나기 전에는 다음 마일스톤을 시작하지 않는다.
