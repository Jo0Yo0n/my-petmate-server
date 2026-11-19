-- Local development fixture. Run after Flyway migration V5.
-- From the repository root (with the Compose PostgreSQL service running):
-- docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < scripts/fixtures/ansan-regions.sql
-- Sample only: four legal neighborhoods, not the entire Ansan catalog.
-- IDs match the official catalog: UUIDv5(namespace, 10-digit legal region code).
-- Codes: 4100000000, 4127100000, 4127300000, 4127110100, 4127110200, 4127310100, 4127310700.
-- Source: https://www.data.go.kr/data/15077871/openapi.do
-- District names include the city to preserve the three-level catalog hierarchy.

BEGIN;

INSERT INTO region (id, name, level, parent_id)
VALUES ('d5c56a23-2c5e-5405-84b3-1b8c7c658c75', '경기도', 'sido', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO region (id, name, level, parent_id)
VALUES ('c328470e-f1ce-5c02-9349-5cb331c61fc6', '안산시 상록구', 'sgg', 'd5c56a23-2c5e-5405-84b3-1b8c7c658c75'),
       ('862f29b3-3ed6-5c33-983f-6683ce04fb8b', '안산시 단원구', 'sgg', 'd5c56a23-2c5e-5405-84b3-1b8c7c658c75')
ON CONFLICT (id) DO NOTHING;

INSERT INTO region (id, name, level, parent_id)
VALUES ('c6cd82c5-907c-50f8-b341-44c379916e78', '일동', 'umd', 'c328470e-f1ce-5c02-9349-5cb331c61fc6'),
       ('e17a6e67-2cc5-58e9-be85-fd029ea4f53a', '이동', 'umd', 'c328470e-f1ce-5c02-9349-5cb331c61fc6'),
       ('c6a667c5-fdf8-5558-a3f8-0eb7ea59e4db', '고잔동', 'umd', '862f29b3-3ed6-5c33-983f-6683ce04fb8b'),
       ('f6a24e5f-a58a-563e-b7f8-e3b4d4ed7673', '초지동', 'umd', '862f29b3-3ed6-5c33-983f-6683ce04fb8b')
ON CONFLICT (id) DO NOTHING;

COMMIT;
