# My Petmate Server

반려견 보호자가 산책·놀이 친구를 안전하게 찾고, 매칭 이후 약속까지 조율할 수 있도록 만드는 **My Petmate MVP의 백엔드 API**입니다.

이 프로젝트는 단순히 후보를 나열하는 서비스가 아니라, 안전 규칙과 개인정보 보호를 먼저 적용한 뒤 반려견 간 궁합을 안내하는 것을 목표로 합니다. 현재 Spring Boot와
PostgreSQL 기반의 인증 토대 (M1)를 구현 중입니다.

## 제품 흐름

```text
보호자·반려견 등록
        ↓
안전 제외 → 공개 정보 상호성 → 선택 필터 → 궁합 안내
        ↓
상호 좋아요로만 매칭 생성
        ↓
멱등 WebSocket 채팅 · 약속 조율 · 종료 후 비공개 평가
```

한 보호자 계정은 여러 반려견을 등록할 수 있지만, 추천·좋아요·매칭·채팅은 항상 선택한 반려견 한 쌍을 기준으로 동작합니다.

## 주요 설계

| 주제                | 설계 판단                                                                                                                       |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------|
| 안전 우선 추천      | 차단·신고·명백한 부적합 후보를 먼저 제외합니다. 클라이언트의 선호 필터는 이 규칙을 완화할 수 없습니다.                          |
| 개인정보 보호       | 정확한 실시간 위치 대신 근사 거리와 활동 지역만 다룹니다. 보호자 정보는 공개 설정과 상호성 조건을 모두 만족할 때만 노출합니다.  |
| 신뢰할 수 있는 인증 | 이메일·비밀번호 인증에 JWT access token과 회전되는 refresh token을 사용하며, 비밀번호와 refresh token 원문은 저장하지 않습니다. |
| 예측 가능한 API     | REST 오류는 RFC 9457 `ProblemDetail` 형식과 요청 ID를 일관되게 사용합니다. API 계약은 OpenAPI로 관리합니다.                     |
| 재전송에 강한 채팅  | 과거 메시지는 cursor pagination으로 조회하고, WebSocket 전송은 클라이언트 멱등성 키와 명시적인 성공·실패 이벤트를 사용합니다.   |
| 제한적인 공개 평가  | 약속이 끝난 뒤 참여자가 상대에게 한 번 남기는 평가는 비공개이며, 원문과 집계 점수도 상대에게 공개하지 않습니다.                 |

## 기술 구성

| 구분            | 선택                                            |
|-----------------|-------------------------------------------------|
| 언어·프레임워크 | Java 21 LTS, Spring Boot 3.x                    |
| 빌드·형식       | Gradle Kotlin DSL, Spotless, Google Java Format |
| 데이터          | PostgreSQL, Flyway, JPA                         |
| 보안·통신       | Spring Security, JWT, REST, WebSocket           |
| 테스트          | JUnit, Spring Boot Test, Spring Security Test   |

## 구현 현황

현재 **M1 — 인증 기반**을 진행 중입니다. 이 단계에서는 회원가입·로그인·토큰 갱신·로그아웃·현재 보호자 조회/수정과 공통 오류·요청 ID를 완성합니다. 이후 반려견
프로필, 추천, 매칭, 채팅, 약속·평가, 안전 조치를 순서대로 확장합니다.

세부 범위와 완료 기준은 [백엔드 MVP 마일스톤 계획](./backend-mvp-plan.md)에서 확인할 수 있습니다.

## 문서 지도

- 제품 요구사항과 인수 기준: [seed.yaml](./seed.yaml)
- REST API 계약: [openapi.yaml](./openapi.yaml)
- 오류와 요청 ID 계약: [error-response.md](./error-response.md)
- WebSocket 계약: [websocket-protocol.md](./websocket-protocol.md)
- 개발·검증 규칙: [development-conventions.md](./development-conventions.md)
- 상세 문서 안내와 기능별 읽기 순서: [documentation-guide.md](./documentation-guide.md)

## 로컬 실행

로컬 PostgreSQL을 실행하려면 `.env`에 `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_PORT`를 설정한 뒤 다음을 실행합니다.

```bash
docker compose up -d postgres
# 최초 한 번만 `openssl rand -base64 32`으로 생성해 안전하게 보관한 값을 설정한다.
export JWT_SECRET="<stored-jwt-secret>"
./gradlew bootRun --args='--spring.profiles.active=local'
```

`JWT_SECRET`은 HS256 access token 서명·검증에 사용하는 외부 환경 변수다. 최소 32바이트여야 하며, Git이나 application YAML에 실제 값을 저장하지 않는다. 서버 재시작에도 같은 값을 사용한다.

검증은 다음 명령으로 실행합니다.

```bash
./gradlew check
```
