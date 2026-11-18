-- Local development fixture. Run after Flyway migration V5.
-- From the repository root (with the Compose PostgreSQL service running):
-- docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < scripts/fixtures/ansan-regions.sql
-- Sample only: four administrative neighborhoods, not the entire Ansan catalog.
-- IDs are fixed UUIDs for repeatable fixture execution, not official administrative codes.
-- Source: https://www.ansan.go.kr/stat/common/cntnts/selectContents.do?cntnts_id=C0001013
-- District names include the city to preserve the three-level catalog hierarchy.

BEGIN;

INSERT INTO region (id, name, level, parent_id)
VALUES ('fa77a358-f57b-4eb8-8491-984e0ae7cc65', '경기도', 'province', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO region (id, name, level, parent_id)
VALUES ('2d032bc3-37d8-4ab3-b634-c1cd5d472eb4', '안산시 상록구', 'district', 'fa77a358-f57b-4eb8-8491-984e0ae7cc65'),
       ('492c66cd-d012-4149-97eb-a832f0252132', '안산시 단원구', 'district', 'fa77a358-f57b-4eb8-8491-984e0ae7cc65')
ON CONFLICT (id) DO NOTHING;

INSERT INTO region (id, name, level, parent_id)
VALUES ('1997243c-3379-4113-985f-12a306295669', '일동', 'locality', '2d032bc3-37d8-4ab3-b634-c1cd5d472eb4'),
       ('c39f2f04-fcde-404c-a459-f4845cbf888e', '이동', 'locality', '2d032bc3-37d8-4ab3-b634-c1cd5d472eb4'),
       ('137455a6-38b2-44af-ae72-cedbcffb1629', '고잔동', 'locality', '492c66cd-d012-4149-97eb-a832f0252132'),
       ('0efdc187-bd92-4aa4-8fa9-cb7f34bc9753', '초지동', 'locality', '492c66cd-d012-4149-97eb-a832f0252132')
ON CONFLICT (id) DO NOTHING;

COMMIT;
