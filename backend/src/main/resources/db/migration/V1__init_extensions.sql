-- PostGIS: geography(Point), geography(LineString) 타입 및 공간 함수 활성화
CREATE EXTENSION IF NOT EXISTS postgis;

-- pgcrypto: gen_random_uuid(), digest() 함수 활성화
-- gen_random_uuid()는 PostgreSQL 13+에서 내장 함수이지만,
-- pgcrypto를 명시하여 암호화 함수를 추가로 활성화한다
CREATE EXTENSION IF NOT EXISTS pgcrypto;
