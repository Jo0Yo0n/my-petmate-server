# 백엔드 MVP 계획

이 문서는 `docs/seed.yaml`을 사람이 한 마일스톤씩 직접 구현할 수 있을 만큼 작은 백엔드 전용 작업으로 나눈다. 이 문서는 의도적으로 계획 문서이며, 비즈니스 코드가 아니다.

## 진행 기준

- 각 마일스톤은 이전 마일스톤의 테스트와 짧은 수동 API 확인이 끝난 뒤 시작한다.
- 날짜 대신 완료 조건을 기준으로 진행하며 한 번에 하나의 마일스톤만 `진행 중`으로 둔다.

| 마일스톤 | 상태 | 범위 판단 |
| --- | --- | --- |
| M0. 백엔드 스캐폴드 | 완료 | 비즈니스 로직 없이 빌드와 기본 의존성까지만 둔다. |
| M1. 인증 기반 | 진행 중 | JWT, refresh token, 현재 보호자 조회와 개인/커플/가족 공개 설정까지 한 묶음으로 구현해야 이후 API 권한·세션 복원·추천 필터 테스트가 가능하다. |
| M2. 반려견 프로필 | 예정 | 추천, like, match의 입력 데이터와 서버 관리 사진 업로드가 되므로 M3 전에 끝낸다. |
| M3. 추천 | 예정 | 실시간 배치 없이 요청 시점 계산과 선택적 선호 필터를 구현한다. 안전 제외는 필터보다 먼저 적용하고 차단/신고 제외는 M7에서 완성한다. |
| M4. Like와 Match | 예정 | 상호 like, match 생성, 차단과 구분되는 match 종료까지 다루고 채팅은 포함하지 않는다. |
| M5. 채팅 | 예정 | cursor 내역 조회, 멱등 WebSocket 전송, 읽음 위치와 권한 검증을 포함하고 appointment card 상태 처리는 M6로 넘긴다. |
| M6. 약속과 평가 | 예정 | 채팅 안의 약속 카드 상태 전이와 완료된 약속의 비공개 상대 평가를 연결한다. |
| M7. 안전 조치 | 예정 | 차단/신고 API와 추천/채팅 제외를 완성하되, 관리자 검토 화면은 MVP 밖으로 둔다. |

## 현재 위치

현재 프로젝트는 M0를 완료했으며 M1 인증 기반을 진행 중이다.

AI agent가 현재 마일스톤을 알아야 할 때는 이 섹션과 위 진행 표를 먼저 읽는다. 상태 값은 `진행 중`, `예정`, `완료`만 사용한다. 다음 마일스톤을 시작하기 전에 이전 마일스톤 상태를 `완료`로 바꾸고, 새 마일스톤 하나만 `진행 중`으로 둔다.

## 도메인 모델 초안

| 모델 | 목적 | 주요 필드 |
| --- | --- | --- |
| Guardian | 한 마리 이상의 반려견을 등록하는 계정 소유자 | id, email, password_hash, profile_type, gender, identity_visibility, status, created_at, updated_at |
| RefreshToken | JWT refresh token 수명 주기 관리 | id, guardian_id, token_hash, expires_at, revoked_at, created_at |
| Dog | 매칭에 사용하는 반려견 프로필 | id, guardian_id, name, photo_object_key, breed, age, sex, weight_kg, size_group, neutered, primary_activity_area, approximate_latitude, approximate_longitude, vaccinated, animal_registered, sociability, aggression_level, preferred_walk_intensity, unfamiliar_dog_reaction, self_introduction |
| DogAvailableTimeSlot | 반려견의 반복 가능한 산책 가능 시간대 | id, dog_id, day_of_week, start_time, end_time |
| DogInteraction | 반려견 쌍 사이의 like 또는 pass | id, from_dog_id, to_dog_id, status, created_at |
| Match | 두 반려견의 상호 like로 생성되는 매칭 | id, dog_a_id, dog_b_id, status, created_at, closed_at |
| ChatMessage | 매칭 채팅방의 텍스트 또는 약속 카드 메시지 | id, match_id, sender_guardian_id, client_message_id, message_type, body, appointment_proposal_id, created_at |
| AppointmentProposal | 산책/놀이 약속 제안 | id, match_id, proposed_by_guardian_id, date, start_time, end_time, place_text, status, cancellation_reason, created_at, decided_at |
| AppointmentReview | 완료된 약속에서 상대 보호자에게 남기는 비공개 평가 | id, appointment_proposal_id, reviewer_guardian_id, target_guardian_id, score, tags, comment, created_at |
| SafetyAction | 차단 또는 신고 조치 | id, reporter_guardian_id, target_guardian_id, type, reason, comment, created_at |
| MatchReadState | 보호자별 채팅 읽음 위치 | match_id, guardian_id, last_read_message_id, updated_at |

## API 초안

| 영역 | Method and Path | 목적 |
| --- | --- | --- |
| Auth | `POST /api/auth/signup` | 보호자 계정 생성 |
| Auth | `POST /api/auth/login` | access token과 refresh token 발급 |
| Auth | `POST /api/auth/refresh` | access token 갱신 |
| Auth | `POST /api/auth/logout` | refresh token 폐기 |
| Guardians | `GET /api/guardians/me` | 현재 인증된 보호자와 계정 상태 조회 |
| Guardians | `PATCH /api/guardians/me` | 개인/커플/가족 유형, 개인 성별과 공개 여부 수정 |
| Dogs | `POST /api/dogs` | `profile` JSON과 초기 `photo`를 multipart로 받아 반려견 프로필 생성 |
| Dogs | `GET /api/dogs` | 내 반려견 목록 조회 |
| Dogs | `GET /api/dogs/{dogId}` | 내 반려견 프로필 조회 |
| Dogs | `PATCH /api/dogs/{dogId}` | 내 반려견 프로필 수정 |
| Dogs | `POST /api/dogs/{dogId}/photos` | 소유한 반려견의 사진 교체 |
| Recommendations | `GET /api/dogs/{dogId}/recommendations` | 반려견 조건과 공개된 보호자 정보의 선택적 선호 query를 적용해 요청 시점에 추천 목록 계산 |
| Likes | `POST /api/dogs/{dogId}/likes` | 추천 반려견에 like 또는 pass |
| Matches | `GET /api/matches` | 최근 메시지와 읽지 않은 수를 포함한 활성 매칭 목록 조회 |
| Matches | `GET /api/matches/{matchId}` | 매칭 상세 조회 |
| Matches | `POST /api/matches/{matchId}/close` | 차단·신고 없이 활성 매칭 종료 |
| Chat | `GET /api/matches/{matchId}/messages?before={cursor}&limit={limit}` | cursor 기반 채팅 내역 역방향 조회 |
| Chat | `POST /api/matches/{matchId}/read` | 현재 보호자의 마지막 읽은 메시지 갱신 |
| Chat | `WS /ws` | `clientMessageId` 기반 멱등 전송과 성공·실패 event 교환 |
| Appointments | `POST /api/matches/{matchId}/appointment-proposals` | 약속 제안 |
| Appointments | `POST /api/appointment-proposals/{proposalId}/accept` | 약속 제안 수락 |
| Appointments | `POST /api/appointment-proposals/{proposalId}/reject` | 약속 제안 거절 |
| Appointments | `POST /api/appointment-proposals/{proposalId}/cancel` | 수락된 약속 취소 |
| Appointments | `POST /api/appointment-proposals/{proposalId}/review` | 종료 시각이 지난 수락 약속의 상대 보호자 평가 |
| Safety | `POST /api/guardians/{guardianId}/block` | 보호자 차단 |
| Safety | `POST /api/guardians/{guardianId}/reports` | 보호자 신고 |

### 이번 범위의 7개 API 계약 추가·변경

1. 현재 보호자와 공개 설정: `GET /api/guardians/me`, `PATCH /api/guardians/me`
2. 반려견 사진: `POST /api/dogs` multipart 초기 사진과 `POST /api/dogs/{dogId}/photos` 교체
3. 추천 선호 조건과 공개 상호성: 기존 `GET /api/dogs/{dogId}/recommendations`의 반려견 조건, 공개 보호자 정보 query와 요청자 공개 상태에 따른 후보 제한
4. 매칭 종료: `POST /api/matches/{matchId}/close`
5. 신뢰 가능한 채팅: 기존 메시지 내역의 cursor pagination과 `WS /ws` 멱등 event 계약
6. 읽지 않은 메시지: `GET /api/matches` 응답 확장과 `POST /api/matches/{matchId}/read`
7. 약속 후 평가: `POST /api/appointment-proposals/{proposalId}/review`와 약속 카드의 평가 가능 상태

한 항목이 여러 endpoint를 함께 바꾸는 경우에도 하나의 사용자 기능 계약으로 센다. 특히 안전 제외는 추천 선호 조건보다 먼저 적용하며, match close는 block/report와 다른 상태 전이다.

### 보호자 정보 공개 상호성 정책

추천 후보는 `안전·차단·신고 제외 → 공개 상호성 → 사용자가 선택한 선호 조건 → 궁합 정렬` 순서로 결정한다.

1. 요청 보호자가 정보를 공개하면 정보를 공개한 보호자만 추천한다.
2. 공개 요청자가 `guardianIdentities`를 사용하면 공개 후보 중 선택한 여성·남성·커플·가족 정보와 일치하는 후보만 남긴다.
3. 요청 보호자가 정보를 비공개하면 공개·비공개 후보를 모두 추천할 수 있지만 모든 후보의 `guardianIdentity`를 응답에서 마스킹한다.
4. 비공개 요청자는 `guardianIdentities`를 사용할 수 없으며 서버는 `IDENTITY_DISCLOSURE_REQUIRED`를 반환한다.
5. 공개 여부 변경은 다음 추천 요청부터 적용하며 이미 생성된 match와 기존 chat에는 영향을 주지 않는다.

## DB 테이블 초안

| 테이블 | 비고 |
| --- | --- |
| guardians | 고유 email, password hash, 개인/커플/가족 유형, 개인일 때의 gender, 공개 여부와 status 저장 |
| refresh_tokens | token hash만 저장하고 guardian과 연결, 폐기 시각 저장 |
| dogs | 한 guardian이 여러 dog를 소유한다. 정확한 실시간 위치가 아니라 활동 지역과 근사 좌표만 저장하고 사진 객체 key를 연결한다. |
| dog_available_time_slots | 한 dog가 여러 가능 시간대를 가진다. |
| dog_interactions | `(from_dog_id, to_dog_id)` 고유. status는 liked 또는 passed. |
| matches | 순서 없는 dog 쌍 기준 고유. active, blocked, closed 상태를 가진다. |
| chat_messages | match와 메시지를 보낸 guardian에 연결된다. `(match_id, sender_guardian_id, client_message_id)`는 고유하다. |
| match_read_states | `(match_id, guardian_id)`별 마지막 읽은 메시지를 저장한다. |
| appointment_proposals | match에 연결된다. 상태 흐름은 proposed, accepted, rejected, canceled. |
| appointment_reviews | `(appointment_proposal_id, reviewer_guardian_id)`가 고유하다. 수락된 약속의 종료 시각 이후 상대 보호자에게 남긴 비공개 평가를 저장한다. |
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
- `GET /api/guardians/me`
- `PATCH /api/guardians/me`

작업:

- guardian과 refresh token migration을 정의한다.
- signup, login, refresh, logout과 현재 보호자 조회·공개 설정 수정을 구현한다.
- signup과 `PATCH /api/guardians/me`는 `profileType`, 개인 계정일 때의 `gender`, `identityVisibility`를 받는다.
- `profileType=individual`이면 `gender`가 필수이며 `female` 또는 `male`만 허용한다. `profileType=couple` 또는 `profileType=family`이면 gender를 받지 않고, 공개 시 카드에는 각각 `커플 보호자`, `가족 보호자`로 표시한다.
- `identityVisibility=private`이면 추천 응답에 성별·커플·가족 유형을 노출하지 않는다.
- 비밀번호는 hash로 저장하고 refresh token은 hash만 저장한다.
- REST 오류는 `docs/openapi.yaml`과 `docs/error-response.md`의 RFC 9457 `ProblemDetail` 계약을 사용하고 `application/problem+json`으로 반환한다.
- Spring MVC advice와 Spring Security의 `AuthenticationEntryPoint`·`AccessDeniedHandler`가 같은 `type`, `title`, `status`, `detail`, `instance`, `code`, 선택적 `fieldErrors`, `requestId` 구조를 사용한다.

완료 기준:

- 보호자가 회원가입, 로그인, 토큰 갱신, 로그아웃을 할 수 있다.
- 앱이 유효한 access token으로 보호자 id, email, profileType, gender, identityVisibility, status를 복원할 수 있다.
- 보호자가 개인/커플/가족 유형과 공개 여부를 변경할 수 있으며 다른 보호자에게는 공개용 파생 값만 노출된다.
- 보호된 테스트 endpoint가 access token 누락/오류 요청을 거부한다.
- validation, 인증, 권한, 리소스 없음, 상태 충돌 오류가 OpenAPI의 `ProblemDetail` 형식과 일치한다.

테스트 기준:

- token 생성과 refresh 검증 단위 테스트.
- signup/login/refresh/logout/me 조회·수정 정상 흐름과 잘못된 인증 정보 흐름 API 테스트.
- 개인 계정 gender 필수와 여성·남성 enum 제한, 커플·가족 계정 gender 금지, 비공개 응답 마스킹 validation 테스트.
- MVC validation 오류와 Security 401·403의 `application/problem+json`, body status, code, requestId 계약 테스트.

### M2. 반려견 프로필

구현 API:

- `POST /api/dogs`
- `GET /api/dogs`
- `GET /api/dogs/{dogId}`
- `PATCH /api/dogs/{dogId}`
- `POST /api/dogs/{dogId}/photos`

작업:

- dog와 availability migration을 정의한다.
- `POST /api/dogs`는 `profile` JSON과 초기 `photo` 파일을 받는 multipart 계약으로 구현한다.
- 반려견 프로필 생성, 목록 조회, 단건 조회, 수정, 사진 교체 API를 구현한다.
- `breed`는 서버가 허용하는 견종 코드 하나를 받으며 `mixed_breed`를 일반 견종과 동일한 선택지로 제공한다. 별도 믹스견 boolean은 저장하지 않는다.
- 나이는 만 나이 정수 `age` 하나로 통일하며 0~30을 허용하고 1살 미만은 0으로 받는다.
- 사진은 AWS S3 비공개 버킷에 저장하고 응답에는 만료되는 조회 URL만 제공한다.
- 허용 media type, 최대 크기와 이미지 dimensions를 검증하고 업로드·DB 저장 실패 시 객체를 정리한다.
- 필수 안전/궁합 필드를 검증한다.

완료 기준:

- 한 보호자가 여러 반려견을 관리할 수 있다.
- 보호자가 초기 사진을 포함해 프로필을 생성하고 나중에 사진을 교체할 수 있다.
- 보호자가 다른 보호자의 반려견을 조회하거나 수정할 수 없다.

테스트 기준:

- create/list/read/edit API 테스트.
- 초기 사진 업로드와 사진 교체 정상·실패·권한 API 테스트.
- DB 저장 실패 시 업로드 객체가 남지 않는 persistence 테스트.
- 필수 필드 누락과 잘못된 enum 값 검증 테스트.
- 다른 소유자의 데이터 접근 차단 테스트.

### M3. 추천

구현 API:

- `GET /api/dogs/{dogId}/recommendations?sizeGroups=&walkIntensities=&sociabilityLevels=&availableDays=&maxDistanceKm=&guardianIdentities=`

작업:

- 선택된 한 반려견 기준으로 요청 시점 추천 계산을 구현한다.
- 위험한 조합과 차단/신고된 보호자를 제외한다.
- 선택적인 크기, 산책 강도, 사회성, 가능 요일, 최대 근사 거리 선호 조건을 적용한다.
- 선택적인 공개 보호자 정보 `guardianIdentities`는 `female`, `male`, `couple`, `family`의 복수 선택을 지원한다.
- 공개 상호성 규칙을 적용한다. 요청 보호자의 `identityVisibility=public`이면 `identityVisibility=public`인 후보만 추천하고 공개된 `guardianIdentity`를 반환한다.
- 요청 보호자가 비공개이면 기본 추천에는 공개·비공개 후보를 모두 포함할 수 있지만 모든 후보의 `guardianIdentity`를 마스킹한다.
- 비공개 요청자가 `guardianIdentities`를 보내면 `IDENTITY_DISCLOSURE_REQUIRED` validation 오류를 반환한다. 보호자 성별·커플·가족 필터는 자신의 정보도 공개한 요청자만 사용할 수 있다.
- 공개 요청자가 `guardianIdentities`를 지정하면 공개 후보 중 선택값과 일치하는 후보만 남긴다.
- 안전·차단·신고 제외를 먼저 적용한 뒤 선호 조건으로 후보를 좁힌다. 선호 query는 안전 규칙을 완화할 수 없다.
- 숫자 퍼센트 점수가 아니라 compatibility grade와 사람이 읽을 수 있는 이유를 반환한다.
- 목록·상세 응답에는 공개된 경우에만 `guardianIdentity`를 반환한다. 앱은 null이면 `보호자 정보 비공개`로 표시한다.

완료 기준:

- 위험하거나 명확히 맞지 않는 후보는 정렬 전에 제외된다.
- 선호 query가 없으면 안전 조건을 통과한 전체 후보를 반환하고, 일부 query만 있으면 해당 차원만 제한한다.
- 공개 요청자는 필터 유무와 관계없이 공개 후보만 추천받으며, 특정 보호자 정보를 선택하면 그중 일치 후보만 반환한다.
- 비공개 요청자는 보호자 정보 필터를 사용할 수 없고 추천 카드에서 모든 후보의 보호자 정보가 비공개로 보인다.
- 결과에는 근사 거리, 추천 이유와 공개가 허용된 보호자 정보만 표시되며 정확한 위치와 비공개 보호자 정보는 포함하지 않는다.

테스트 기준:

- 제외 규칙 단위 테스트.
- 각 선호 query와 복합 query, 잘못된 enum·거리 값 validation 테스트.
- 공개 요청자의 공개 후보 전용 추천, 공개 여성·남성·커플·가족 필터와 비공개 후보 제외 테스트.
- 비공개 요청자의 기본 후보 유지, guardian identity 전체 마스킹과 `IDENTITY_DISCLOSURE_REQUIRED` 오류 테스트.
- 선호 query가 안전 제외를 우회하지 못함을 증명하는 테스트.
- 추천 목록과 상세 응답 형태 API 테스트.
- 정확한 실시간 위치가 반환되지 않음을 증명하는 privacy 테스트.
- 비공개 보호자의 profile type과 gender가 추천 응답에서 노출되지 않음을 증명하는 privacy 테스트.

### M4. Like와 Match

구현 API:

- `POST /api/dogs/{dogId}/likes`
- `GET /api/matches`
- `GET /api/matches/{matchId}`
- `POST /api/matches/{matchId}/close`

작업:

- 반려견 쌍에 대한 like/pass를 구현한다.
- 양쪽이 모두 like한 뒤에만 match를 생성한다.
- 같은 방향의 반려견 쌍 interaction 중복을 막는다.
- 참여 보호자가 active match를 차단·신고 없이 closed로 전환할 수 있게 한다.
- close는 멱등하게 처리하고 closed match는 활성 목록과 이후 채팅에서 제외한다.

완료 기준:

- 한쪽만 like한 상태에서는 match가 생성되지 않는다.
- 상호 like가 발생하면 active match가 정확히 하나 생성된다.
- 참여자는 active match를 종료할 수 있고, 비참여자는 종료할 수 없다.

테스트 기준:

- 상호 like에 따른 match 생성 service 테스트.
- like/pass API 테스트.
- 중복 interaction 처리 constraint 테스트.
- match close 정상·멱등·권한 API 테스트.

### M5. 채팅

구현 API:

- `GET /api/matches/{matchId}/messages?before={cursor}&limit={limit}`
- `POST /api/matches/{matchId}/read`
- `WS /ws`

작업:

- match 범위의 메시지 내역을 `before` cursor와 `limit`으로 역방향 조회한다.
- `GET /api/matches`에 최근 메시지 미리보기, 최근 메시지 시각, 현재 보호자의 읽지 않은 수를 추가한다.
- 보호자별 마지막 읽은 메시지를 갱신하는 API를 구현한다.
- WebSocket 연결 직후 access token을 검증하고 인증 성공 event 전에는 전송을 받지 않는다.
- client 전송에는 `matchId`, `clientMessageId`, `body`를 포함하고 server는 `message_created` 또는 `message_error`를 반환한다.
- `(match_id, sender_guardian_id, client_message_id)` 고유 제약으로 재전송을 멱등 처리한다.
- 매칭된 보호자만 메시지를 읽거나 보낼 수 있게 제한한다.
- closed 또는 blocked match는 메시지 조회, 읽음 갱신, 전송을 거부한다.

완료 기준:

- 매칭된 보호자가 텍스트 메시지를 주고받을 수 있다.
- 앱이 이전 메시지를 page 단위로 조회하고 재연결 후 중복 없이 전송 결과를 복구할 수 있다.
- 매칭 목록에서 최근 메시지와 읽지 않은 수를 확인하고 채팅 진입 시 읽음 위치를 갱신할 수 있다.
- 참여자가 아닌 사용자는 match chat에 접근할 수 없다.

테스트 기준:

- cursor 경계, limit validation, 역방향 메시지 내역과 권한 API 테스트.
- 읽음 위치 갱신, unread count 계산, 다른 참여자의 read state 격리 테스트.
- 텍스트 메시지 정상 흐름, 인증 실패, 명시적 success/error event WebSocket integration 테스트.
- 같은 `clientMessageId` 재전송이 메시지를 중복 저장하지 않는 테스트.
- closed/blocked match의 조회·전송·읽음 갱신 차단 테스트.

### M6. 약속과 평가

구현 API:

- `POST /api/matches/{matchId}/appointment-proposals`
- `POST /api/appointment-proposals/{proposalId}/accept`
- `POST /api/appointment-proposals/{proposalId}/reject`
- `POST /api/appointment-proposals/{proposalId}/cancel`
- `POST /api/appointment-proposals/{proposalId}/review`

작업:

- 약속 제안, 수락, 거절, 취소 흐름을 구현한다.
- 약속 시간은 서버가 종료 시각을 판단할 수 있도록 `date`, `startTime`, `endTime`으로 받으며 `startTime < endTime`을 검증한다. MVP의 시간대는 `Asia/Seoul`로 고정한다.
- 수락된 약속을 취소할 때 cancellation reason을 저장한다.
- 수락된 약속의 날짜와 종료 시각이 지난 뒤에만 참여 보호자가 상대 보호자를 평가할 수 있게 한다.
- 평가 요청은 `score` 1~5, 선택적인 `tags`, 선택적인 `comment`를 받는다.
- tags는 `on_time`, `respectful_communication`, `safe_handling`, `dog_profile_accurate`, `would_meet_again`만 허용한다.
- `(appointment_proposal_id, reviewer_guardian_id)` 고유 제약으로 보호자별 한 번만 평가하게 한다.
- 채팅의 약속 카드 응답에 `reviewEligible`, `reviewSubmitted`를 포함한다.
- 평가 원문과 집계 점수는 MVP에서 상대 사용자에게 공개하지 않고 운영 품질·안전 분석에만 사용한다.

완료 기준:

- 상대 보호자가 약속 제안을 수락하거나 거절할 수 있다.
- 수락된 약속은 사유와 함께 취소할 수 있다.
- 각 참여 보호자는 수락된 약속의 종료 시각 이후 상대 보호자를 한 번 평가할 수 있다.
- 제안·거절·취소된 약속과 아직 끝나지 않은 약속은 평가할 수 없다.

테스트 기준:

- 상태 전이 테스트.
- 약속 제안 수명 주기 API 테스트.
- 종료 시각 전·후 평가 가능 여부와 1~5점 validation 테스트.
- 보호자별 중복 평가 고유 제약과 비참여자·자기 자신 평가 차단 테스트.
- 평가가 API 응답이나 상대 화면에 공개되지 않음을 확인하는 privacy 테스트.
- 참여자가 아닌 사용자의 접근 차단 테스트.

### M7. 안전 조치

구현 API:

- `POST /api/guardians/{guardianId}/block`
- `POST /api/guardians/{guardianId}/reports`

작업:

- 차단과 신고 API를 구현한다.
- 신고는 `inappropriate_language`, `harassment`, `threatening_behavior`, `unsafe_dog_handling`, `false_profile`, `no_show`, `other` 중 하나의 `reason`과 선택적인 최대 1000자 `comment`를 받는다.
- 사용자가 severity를 정하지 않으며, 접근 제한 판단은 신고 누적과 서버가 분류한 고위험 reason을 기준으로 한다.
- 차단/신고된 보호자를 추천과 채팅 상호작용에서 제외한다.
- 누적 신고 또는 서버가 분류한 고위험 신고에 대해 matching/chat 접근을 임시 제한한다.

완료 기준:

- 차단은 이후 추천과 채팅 상호작용을 막는다.
- 누적 신고 또는 고위험 reason의 신고는 matching/chat 접근 제한으로 이어질 수 있다.

테스트 기준:

- block/report API 테스트.
- 신고 reason enum, 선택 comment, 사용자 입력 severity 금지 validation 테스트.
- 추천 제외 테스트.
- 차단 이후 채팅 접근 테스트.

## 구현 순서

1. M0 scaffold
2. M1 auth foundation
3. M2 dog profiles
4. M3 recommendations
5. M4 likes and matches
6. M5 chat
7. M6 appointments and private reviews
8. M7 safety actions

이전 마일스톤의 테스트가 통과하고 짧은 수동 API 확인이 끝나기 전에는 다음 마일스톤을 시작하지 않는다.
