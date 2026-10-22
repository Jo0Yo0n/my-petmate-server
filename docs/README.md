# My Petmate Server

반려견 보호자가 산책·놀이 친구를 안전하게 찾고, 매칭 이후 약속까지 조율할 수 있도록 만드는 **My Petmate MVP의 백엔드 API**입니다.

이 프로젝트는 단순히 후보를 나열하는 서비스가 아니라, 안전 규칙과 개인정보 보호를 먼저 적용한 뒤 반려견 간 궁합을 안내하는 것을 목표로 합니다. 현재 Spring Boot와
PostgreSQL 기반의 인증 토대 (M1)를 구현 중입니다.

## 제품 흐름

```text
보호자·반려견 등록
        ↓
안전 제외 → 양쪽 찾기 상태 → 선택 필터 → 궁합 안내
        ↓
상호 좋아요로만 매칭 생성
        ↓
멱등 WebSocket 채팅 · 약속 조율 · 종료 후 비공개 평가
```

한 보호자 계정은 여러 반려견을 등록할 수 있지만, 추천·좋아요·매칭·채팅은 항상 선택한 반려견 한 쌍을 기준으로 동작합니다.

## 주요 설계

| 주제                | 설계 판단                                                                                                                                           |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| 안전 우선 추천      | 차단·신고·명백한 부적합 후보를 먼저 제외합니다. 클라이언트의 선호 필터는 이 규칙을 완화할 수 없습니다.                                              |
| 개인정보 보호       | 정확한 실시간 위치 대신 근사 거리와 활동 지역만 다룹니다. 추천은 양 사용자 모두 찾는 중일 때 제공하며 기존 받은 관심 응답은 쉬는 중에도 가능합니다. |
| 신뢰할 수 있는 인증 | 이메일·비밀번호 인증에 JWT access token과 회전되는 refresh token을 사용하며, 비밀번호와 refresh token 원문은 저장하지 않습니다.                     |
| 예측 가능한 API     | REST 오류는 RFC 9457 `ProblemDetail` 형식과 요청 ID를 일관되게 사용합니다. API 계약은 OpenAPI로 관리합니다.                                         |
| 재전송에 강한 채팅  | 과거 메시지는 cursor pagination으로 조회하고, WebSocket 전송은 클라이언트 멱등성 키와 명시적인 성공·실패 이벤트를 사용합니다.                       |
| 제한적인 공개 평가  | 약속이 끝난 뒤 참여자가 상대에게 한 번 남기는 평가는 비공개이며, 원문과 집계 점수도 상대에게 공개하지 않습니다.                                     |

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

로컬 실행 전에 `.env.example`을 복사해 `.env`를 만들고, DB 설정을 확인합니다. `JWT_SECRET`은 `openssl rand -base64 32`로 한 번 생성해 `.env`에 저장하고 계속
재사용합니다.

```bash
cp .env.example .env
openssl rand -base64 32
# 출력값을 .env의 JWT_SECRET에 복사한다.
docker compose up -d postgres
./gradlew bootRun
```

Gradle `bootRun`은 별도 프로파일 지정이 없으면 `local`을 사용한다. `application-local.yaml`은 활성화될 때 `.env`를 읽으므로 별도 `export`가 필요하지 않다. 다른
프로파일은 `SPRING_PROFILES_ACTIVE`로 지정할 수 있다. `JWT_SECRET`은 HS256 access token 서명·검증에 사용하는 비밀값으로 최소 32바이트여야 한다. `.env`는
Git에서 제외되며, 실제 값을 application YAML이나 Git에 저장하지 않는다.

검증은 다음 명령으로 실행합니다.

```bash
./gradlew check
```

Schemathesis 계약 검증은 `uvx`로 Python 3.12와 Schemathesis 4.32.1을 사용합니다. 최초 실행 시 필요한 런타임과 패키지를 다운로드하며,
캐시와 런타임은 `.gradle` 아래에 저장합니다.

`schemathesis.toml`에서 `contract_hooks.py` 인증 공급자를 로드합니다. 공급자는 회원가입 API로 고유 이메일의 검증용 계정을 만들고,
응답의 access token을 테스트 요청에 적용합니다. 탈퇴 요청은 매번 별도 계정을 사용해 다른 테스트의 인증 상태를 훼손하지 않습니다.
실행마다 계정이 DB에 추가되므로 개발 DB와 분리한 계약 검증 DB에서 실행합니다. 서버는 테스트 전에 별도로 실행해야 합니다.

전역 차단 메서드의 검증 범위는 [오류 계약](./error-response.md#http-메서드-차단과-계약-적용-범위)을 따릅니다.
별도 보안 회귀 테스트의 입력과 검증 항목은 [M1-SEC-11](./m1-test-cases-detail.md#m1-sec-11)에서 관리합니다.
`./gradlew check`에 포함되며, 단독 실행은 `./gradlew test --tests '*BlockedHttpMethodIntegrationTest'`입니다.
HTTP 헤더에 허용되지 않는 제어문자는 생성 대상에서 제외합니다. 회원가입의 이메일 중복과 프로필의 최초 성별 변경 제한은
상태에 따른 정상적인 409로 검증하며, 응답 상태·본문 스키마 검사는 유지합니다.
자동 생성된 로그인 자격 증명과 refresh token은 실제 계정·token과 일치하지 않을 수 있습니다.
이들의 성공 흐름과 token 수명 주기는 기존 인증 통합 테스트에서 검증합니다.

```bash
./gradlew openApiContractTest
```

인증 공급자의 계정 격리 회귀 검증은 다음 명령으로 실행합니다.

```bash
UV_CACHE_DIR=.gradle/uv-cache UV_PYTHON_INSTALL_DIR=.gradle/uv-python UV_TOOL_DIR=.gradle/uv-tools \
  uvx --python 3.12 --from schemathesis==4.32.1 python -m unittest test_contract_hooks
```
