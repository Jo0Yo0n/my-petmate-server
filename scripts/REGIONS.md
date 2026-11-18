# 서울·경기 법정동 자료 적재

Python 3 기본 라이브러리만 사용한다. 저장·변환 도구이며 서버 API를 구현하거나 DB에 자동 접속하지 않는다.
원천: [행정안전부 법정동코드 API](https://www.data.go.kr/data/15077871/openapi.do).

저장소 루트의 Git 제외 파일 `.env`에 `DATA_GO_KR_SERVICE_KEY`를 설정한다.
공공데이터포털의 인코딩·디코딩 키 모두 지원한다.
셸 환경변수로 내보낸 뒤 수집한다. 이미 저장한 디렉터리는 덮어쓰지 않는다.

```bash
set -a
source .env
set +a
python3 scripts/import_regions.py --output-dir data/regions
```

서울·경기의 모든 페이지를 XML로 보관한다. `manifest.json`에는 출처·UTC 수집 시각·파일별 SHA-256을 기록한다.
중복 코드, 페이지 누락·건수 변경, 코드 구성 불일치, 미지원 부모 관계가 있으면 SQL을 만들지 않는다.
전체 수집·검증 성공 후에만 결과 디렉터리를 확정하며, 실패 시 임시 결과를 정리한다.
API가 페이지 간 스냅샷을 보장하는 것은 아니므로 동일 건수의 변경까지 감지하는 보장은 없다.

인증키나 네트워크 없이도 이미 저장한 XML을 다시 검증하고 SQL을 재생성할 수 있다.

```bash
python3 scripts/import_regions.py --from-xml data/regions
python3 -m unittest discover -s scripts -p 'test_*.py'
```

서울·경기 초기 데이터는 기존 Flyway V5에 포함되어 빈 DB에서 서버를 시작하면 자동 적재된다.
초기 적재에는 API 키나 별도 SQL 실행이 필요 없다. `regions.sql`은 원본으로부터 생성한 별도 재적재용 파일이다.
다음 명령은 이미 마이그레이션을 적용한 개발 DB에 재적재하므로 실행 전에 대상 DB를 확인한다.

```bash
docker compose exec -T postgres sh -c \
  'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < data/regions/regions.sql
```

SQL은 트랜잭션 하나로 실행하며 테이블 잠금으로 동시 적재·수정을 직렬화한다. 일반 SELECT는 허용된다.
실패 시 전체 롤백하고 재실행 시 같은 ID의 이름·계층을 갱신한다. 삭제·폐지 처리는 하지 않는다.
UUID는 스크립트의 고정 namespace와 공식 10자리 코드로 생성하는 UUIDv5다. namespace를 바꾸면 ID도 바뀐다.

시·도는 `sido`, 읍·면·동의 직속 시·군·구는 `sgg`, 읍·면·법정동은 `umd`다. 리는 제외한다.
비자치구가 있는 시는 `경기도 → 안산시 상록구 → 일동`처럼 시·구 이름을 합쳐 3단계로 제공한다.
테스트, 예시에 사용되는 안산 fixture는 공식 자료와 동일한 UUID·이름·계층을 사용한다.
