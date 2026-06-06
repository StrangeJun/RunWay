-- V12: 전국 유명 러닝 코스 시드 데이터 (33개)
-- 시스템 계정 소유, status='published'

INSERT INTO users (id, email, password_hash, nickname, bio)
VALUES ('00000000-0000-0000-0000-000000000001','system@runway.app',
        '$2a$10$SEED_PLACEHOLDER_HASH_NOT_FOR_LOGIN_00000000000000000000',
        'RunWay 공식','전국 유명 러닝 코스를 엄선해 제공합니다.')
ON CONFLICT (id) DO NOTHING;

-- ── 헬퍼: 좌표 → geography point
-- ST_MakePoint(경도, 위도)

-- ===========================================================
-- 서울 (15개)
-- ===========================================================

-- 1. 여의도 한강공원 순환
INSERT INTO courses VALUES ('c0000001-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'여의도 한강공원 순환','여의도 한강공원을 한 바퀴 도는 대표 도심 러닝 코스. 강변 뷰와 넓은 잔디밭이 매력적이며 봄 벚꽃 시즌에 특히 인기입니다. 완전 평탄 노면으로 초보자에게 추천합니다.',
'published',6320,TRUE,
ST_SetSRID(ST_MakePoint(126.9230,37.5265),4326)::geography,
ST_SetSRID(ST_MakePoint(126.9230,37.5265),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.9230,37.5265),ST_MakePoint(126.9310,37.5275),ST_MakePoint(126.9400,37.5282),ST_MakePoint(126.9490,37.5285),ST_MakePoint(126.9580,37.5278),ST_MakePoint(126.9640,37.5265),ST_MakePoint(126.9580,37.5250),ST_MakePoint(126.9490,37.5244),ST_MakePoint(126.9400,37.5247),ST_MakePoint(126.9310,37.5255),ST_MakePoint(126.9230,37.5265)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000001-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.9230,37.5265),4326)::geography),
(gen_random_uuid(),'c0000001-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(126.9310,37.5275),4326)::geography),
(gen_random_uuid(),'c0000001-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(126.9400,37.5282),4326)::geography),
(gen_random_uuid(),'c0000001-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(126.9490,37.5285),4326)::geography),
(gen_random_uuid(),'c0000001-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(126.9580,37.5278),4326)::geography),
(gen_random_uuid(),'c0000001-0000-0000-0000-000000000000',6,ST_SetSRID(ST_MakePoint(126.9640,37.5265),4326)::geography),
(gen_random_uuid(),'c0000001-0000-0000-0000-000000000000',7,ST_SetSRID(ST_MakePoint(126.9490,37.5244),4326)::geography),
(gen_random_uuid(),'c0000001-0000-0000-0000-000000000000',8,ST_SetSRID(ST_MakePoint(126.9310,37.5255),4326)::geography),
(gen_random_uuid(),'c0000001-0000-0000-0000-000000000000',9,ST_SetSRID(ST_MakePoint(126.9230,37.5265),4326)::geography)
ON CONFLICT DO NOTHING;

-- 2. 남산 순환
INSERT INTO courses VALUES ('c0000002-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'서울 남산 순환','도심 한가운데 위치한 남산을 한 바퀴 도는 코스. 오르막·내리막이 있어 체력 향상에 효과적이며 남산타워 전망이 일품입니다.',
'published',7400,TRUE,
ST_SetSRID(ST_MakePoint(126.9872,37.5534),4326)::geography,
ST_SetSRID(ST_MakePoint(126.9872,37.5534),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.9872,37.5534),ST_MakePoint(126.9848,37.5495),ST_MakePoint(126.9870,37.5455),ST_MakePoint(126.9938,37.5448),ST_MakePoint(127.0010,37.5478),ST_MakePoint(127.0028,37.5502),ST_MakePoint(126.9995,37.5540),ST_MakePoint(126.9920,37.5545),ST_MakePoint(126.9872,37.5534)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000002-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.9872,37.5534),4326)::geography),
(gen_random_uuid(),'c0000002-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(126.9848,37.5495),4326)::geography),
(gen_random_uuid(),'c0000002-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(126.9870,37.5455),4326)::geography),
(gen_random_uuid(),'c0000002-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(126.9938,37.5448),4326)::geography),
(gen_random_uuid(),'c0000002-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(127.0010,37.5478),4326)::geography),
(gen_random_uuid(),'c0000002-0000-0000-0000-000000000000',6,ST_SetSRID(ST_MakePoint(127.0028,37.5502),4326)::geography),
(gen_random_uuid(),'c0000002-0000-0000-0000-000000000000',7,ST_SetSRID(ST_MakePoint(126.9995,37.5540),4326)::geography),
(gen_random_uuid(),'c0000002-0000-0000-0000-000000000000',8,ST_SetSRID(ST_MakePoint(126.9872,37.5534),4326)::geography)
ON CONFLICT DO NOTHING;

-- 3. 청계천 코스
INSERT INTO courses VALUES ('c0000003-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'청계천 코스','청계광장에서 출발해 청계천을 따라 달리는 도심 속 힐링 코스. 물소리와 함께 달릴 수 있어 직장인 러너에게 인기입니다.',
'published',8700,FALSE,
ST_SetSRID(ST_MakePoint(126.9782,37.5696),4326)::geography,
ST_SetSRID(ST_MakePoint(127.0318,37.5642),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.9782,37.5696),ST_MakePoint(126.9900,37.5703),ST_MakePoint(127.0020,37.5710),ST_MakePoint(127.0140,37.5710),ST_MakePoint(127.0260,37.5698),ST_MakePoint(127.0318,37.5642)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000003-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.9782,37.5696),4326)::geography),
(gen_random_uuid(),'c0000003-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(126.9900,37.5703),4326)::geography),
(gen_random_uuid(),'c0000003-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.0020,37.5710),4326)::geography),
(gen_random_uuid(),'c0000003-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.0140,37.5710),4326)::geography),
(gen_random_uuid(),'c0000003-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(127.0260,37.5698),4326)::geography),
(gen_random_uuid(),'c0000003-0000-0000-0000-000000000000',6,ST_SetSRID(ST_MakePoint(127.0318,37.5642),4326)::geography)
ON CONFLICT DO NOTHING;

-- 4. 반포 한강공원
INSERT INTO courses VALUES ('c0000004-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'반포 한강공원 코스','반포대교 인근을 달리는 코스. 달빛무지개분수와 야경이 아름다워 야간 러닝으로도 인기입니다.',
'published',6000,FALSE,
ST_SetSRID(ST_MakePoint(126.9938,37.5131),4326)::geography,
ST_SetSRID(ST_MakePoint(127.0395,37.5098),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.9938,37.5131),ST_MakePoint(127.0060,37.5143),ST_MakePoint(127.0180,37.5142),ST_MakePoint(127.0300,37.5122),ST_MakePoint(127.0395,37.5098)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000004-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.9938,37.5131),4326)::geography),
(gen_random_uuid(),'c0000004-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.0060,37.5143),4326)::geography),
(gen_random_uuid(),'c0000004-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.0180,37.5142),4326)::geography),
(gen_random_uuid(),'c0000004-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.0300,37.5122),4326)::geography),
(gen_random_uuid(),'c0000004-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(127.0395,37.5098),4326)::geography)
ON CONFLICT DO NOTHING;

-- 5. 올림픽공원
INSERT INTO courses VALUES ('c0000005-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'올림픽공원 코스','1988 서울올림픽 유산을 간직한 공원 내부를 한 바퀴 도는 코스. 조각 공원과 잔디밭을 통과하며 달립니다.',
'published',5500,TRUE,
ST_SetSRID(ST_MakePoint(127.1241,37.5220),4326)::geography,
ST_SetSRID(ST_MakePoint(127.1241,37.5220),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.1241,37.5220),ST_MakePoint(127.1305,37.5185),ST_MakePoint(127.1388,37.5170),ST_MakePoint(127.1445,37.5205),ST_MakePoint(127.1440,37.5240),ST_MakePoint(127.1388,37.5262),ST_MakePoint(127.1315,37.5255),ST_MakePoint(127.1241,37.5220)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000005-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.1241,37.5220),4326)::geography),
(gen_random_uuid(),'c0000005-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.1305,37.5185),4326)::geography),
(gen_random_uuid(),'c0000005-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.1388,37.5170),4326)::geography),
(gen_random_uuid(),'c0000005-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.1445,37.5205),4326)::geography),
(gen_random_uuid(),'c0000005-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(127.1440,37.5240),4326)::geography),
(gen_random_uuid(),'c0000005-0000-0000-0000-000000000000',6,ST_SetSRID(ST_MakePoint(127.1388,37.5262),4326)::geography),
(gen_random_uuid(),'c0000005-0000-0000-0000-000000000000',7,ST_SetSRID(ST_MakePoint(127.1241,37.5220),4326)::geography)
ON CONFLICT DO NOTHING;

-- 6. 뚝섬 한강공원
INSERT INTO courses VALUES ('c0000006-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'뚝섬 한강공원 코스','뚝섬 한강공원에서 광진교 방향까지 이어지는 강변 코스. 자전거 도로와 분리된 러닝 전용 트랙이 잘 조성되어 있습니다.',
'published',5000,FALSE,
ST_SetSRID(ST_MakePoint(127.0658,37.5302),4326)::geography,
ST_SetSRID(ST_MakePoint(127.1050,37.5225),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.0658,37.5302),ST_MakePoint(127.0760,37.5280),ST_MakePoint(127.0860,37.5260),ST_MakePoint(127.0955,37.5242),ST_MakePoint(127.1050,37.5225)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000006-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.0658,37.5302),4326)::geography),
(gen_random_uuid(),'c0000006-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.0760,37.5280),4326)::geography),
(gen_random_uuid(),'c0000006-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.0860,37.5260),4326)::geography),
(gen_random_uuid(),'c0000006-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.0955,37.5242),4326)::geography),
(gen_random_uuid(),'c0000006-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(127.1050,37.5225),4326)::geography)
ON CONFLICT DO NOTHING;

-- 7. 잠실 한강공원
INSERT INTO courses VALUES ('c0000007-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'잠실 한강공원 코스','잠실 한강공원에서 천호대교까지 이어지는 코스. 롯데타워 전망과 함께 달릴 수 있습니다.',
'published',7000,FALSE,
ST_SetSRID(ST_MakePoint(127.0878,37.5212),4326)::geography,
ST_SetSRID(ST_MakePoint(127.1360,37.5172),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.0878,37.5212),ST_MakePoint(127.0988,37.5198),ST_MakePoint(127.1095,37.5188),ST_MakePoint(127.1210,37.5180),ST_MakePoint(127.1360,37.5172)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000007-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.0878,37.5212),4326)::geography),
(gen_random_uuid(),'c0000007-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.0988,37.5198),4326)::geography),
(gen_random_uuid(),'c0000007-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.1095,37.5188),4326)::geography),
(gen_random_uuid(),'c0000007-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.1210,37.5180),4326)::geography),
(gen_random_uuid(),'c0000007-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(127.1360,37.5172),4326)::geography)
ON CONFLICT DO NOTHING;

-- 8. 망원 한강공원
INSERT INTO courses VALUES ('c0000008-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'망원 한강공원 코스','망원 한강공원에서 난지 방향으로 이어지는 코스. 상암 월드컵경기장 뷰와 함께 달립니다.',
'published',5200,FALSE,
ST_SetSRID(ST_MakePoint(126.8975,37.5537),4326)::geography,
ST_SetSRID(ST_MakePoint(126.8538,37.5672),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.8975,37.5537),ST_MakePoint(126.8878,37.5570),ST_MakePoint(126.8778,37.5605),ST_MakePoint(126.8660,37.5642),ST_MakePoint(126.8538,37.5672)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000008-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.8975,37.5537),4326)::geography),
(gen_random_uuid(),'c0000008-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(126.8878,37.5570),4326)::geography),
(gen_random_uuid(),'c0000008-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(126.8778,37.5605),4326)::geography),
(gen_random_uuid(),'c0000008-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(126.8660,37.5642),4326)::geography),
(gen_random_uuid(),'c0000008-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(126.8538,37.5672),4326)::geography)
ON CONFLICT DO NOTHING;

-- 9. 서울숲
INSERT INTO courses VALUES ('c0000009-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'서울숲 코스','성수동 서울숲 내부를 달리는 코스. 사슴 방목장과 자연 숲길을 통과하며 도심 속 자연을 만끽할 수 있습니다.',
'published',5200,TRUE,
ST_SetSRID(ST_MakePoint(127.0374,37.5449),4326)::geography,
ST_SetSRID(ST_MakePoint(127.0374,37.5449),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.0374,37.5449),ST_MakePoint(127.0418,37.5430),ST_MakePoint(127.0455,37.5415),ST_MakePoint(127.0468,37.5445),ST_MakePoint(127.0452,37.5472),ST_MakePoint(127.0415,37.5480),ST_MakePoint(127.0374,37.5449)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000009-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.0374,37.5449),4326)::geography),
(gen_random_uuid(),'c0000009-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.0418,37.5430),4326)::geography),
(gen_random_uuid(),'c0000009-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.0455,37.5415),4326)::geography),
(gen_random_uuid(),'c0000009-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.0468,37.5445),4326)::geography),
(gen_random_uuid(),'c0000009-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(127.0452,37.5472),4326)::geography),
(gen_random_uuid(),'c0000009-0000-0000-0000-000000000000',6,ST_SetSRID(ST_MakePoint(127.0374,37.5449),4326)::geography)
ON CONFLICT DO NOTHING;

-- 10. 양재천 코스
INSERT INTO courses VALUES ('c0000010-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'양재천 코스','양재에서 탄천 합류 지점까지 이어지는 강변 코스. 버드나무 가로수길과 잘 정비된 산책로가 인상적입니다.',
'published',9500,FALSE,
ST_SetSRID(ST_MakePoint(127.0430,37.4796),4326)::geography,
ST_SetSRID(ST_MakePoint(127.1155,37.4678),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.0430,37.4796),ST_MakePoint(127.0600,37.4762),ST_MakePoint(127.0760,37.4732),ST_MakePoint(127.0920,37.4705),ST_MakePoint(127.1060,37.4688),ST_MakePoint(127.1155,37.4678)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000010-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.0430,37.4796),4326)::geography),
(gen_random_uuid(),'c0000010-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.0600,37.4762),4326)::geography),
(gen_random_uuid(),'c0000010-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.0760,37.4732),4326)::geography),
(gen_random_uuid(),'c0000010-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.0920,37.4705),4326)::geography),
(gen_random_uuid(),'c0000010-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(127.1155,37.4678),4326)::geography)
ON CONFLICT DO NOTHING;

-- 11. 인왕산 코스
INSERT INTO courses VALUES ('c0000011-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'인왕산 코스','경복궁 서쪽 인왕산 자락을 달리는 코스. 한양도성 성곽과 함께 달리며 서울 도심 전망을 즐길 수 있습니다.',
'published',5300,TRUE,
ST_SetSRID(ST_MakePoint(126.9620,37.5820),4326)::geography,
ST_SetSRID(ST_MakePoint(126.9620,37.5820),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.9620,37.5820),ST_MakePoint(126.9645,37.5782),ST_MakePoint(126.9668,37.5748),ST_MakePoint(126.9695,37.5778),ST_MakePoint(126.9680,37.5815),ST_MakePoint(126.9650,37.5842),ST_MakePoint(126.9620,37.5820)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000011-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.9620,37.5820),4326)::geography),
(gen_random_uuid(),'c0000011-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(126.9645,37.5782),4326)::geography),
(gen_random_uuid(),'c0000011-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(126.9668,37.5748),4326)::geography),
(gen_random_uuid(),'c0000011-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(126.9695,37.5778),4326)::geography),
(gen_random_uuid(),'c0000011-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(126.9620,37.5820),4326)::geography)
ON CONFLICT DO NOTHING;

-- 12. 안산 순환
INSERT INTO courses VALUES ('c0000012-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'안산(鞍山) 순환 코스','서대문 안산을 한 바퀴 도는 산악 코스. 도심 접근성이 좋고 비교적 완만한 산길이어서 출퇴근 러닝으로 인기입니다.',
'published',4800,TRUE,
ST_SetSRID(ST_MakePoint(126.9348,37.5890),4326)::geography,
ST_SetSRID(ST_MakePoint(126.9348,37.5890),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.9348,37.5890),ST_MakePoint(126.9310,37.5858),ST_MakePoint(126.9298,37.5825),ST_MakePoint(126.9315,37.5795),ST_MakePoint(126.9355,37.5812),ST_MakePoint(126.9380,37.5848),ST_MakePoint(126.9370,37.5880),ST_MakePoint(126.9348,37.5890)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000012-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.9348,37.5890),4326)::geography),
(gen_random_uuid(),'c0000012-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(126.9310,37.5858),4326)::geography),
(gen_random_uuid(),'c0000012-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(126.9298,37.5825),4326)::geography),
(gen_random_uuid(),'c0000012-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(126.9355,37.5812),4326)::geography),
(gen_random_uuid(),'c0000012-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(126.9348,37.5890),4326)::geography)
ON CONFLICT DO NOTHING;

-- 13. 북서울꿈의숲
INSERT INTO courses VALUES ('c0000013-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'북서울꿈의숲 코스','강북구 번동의 대규모 공원을 달리는 코스. 월영지 호수와 꿈의숲 아트센터 주변을 순환합니다.',
'published',4500,TRUE,
ST_SetSRID(ST_MakePoint(127.0385,37.6418),4326)::geography,
ST_SetSRID(ST_MakePoint(127.0385,37.6418),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.0385,37.6418),ST_MakePoint(127.0420,37.6398),ST_MakePoint(127.0445,37.6372),ST_MakePoint(127.0435,37.6345),ST_MakePoint(127.0405,37.6332),ST_MakePoint(127.0372,37.6348),ST_MakePoint(127.0358,37.6378),ST_MakePoint(127.0385,37.6418)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000013-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.0385,37.6418),4326)::geography),
(gen_random_uuid(),'c0000013-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.0420,37.6398),4326)::geography),
(gen_random_uuid(),'c0000013-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.0445,37.6372),4326)::geography),
(gen_random_uuid(),'c0000013-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.0405,37.6332),4326)::geography),
(gen_random_uuid(),'c0000013-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(127.0358,37.6378),4326)::geography),
(gen_random_uuid(),'c0000013-0000-0000-0000-000000000000',6,ST_SetSRID(ST_MakePoint(127.0385,37.6418),4326)::geography)
ON CONFLICT DO NOTHING;

-- 14. 중랑천 코스
INSERT INTO courses VALUES ('c0000014-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'중랑천 코스','중랑천을 따라 달리는 장거리 강변 코스. 봄철 벚꽃 명소로 유명하며 왕복 20km까지 연장도 가능합니다.',
'published',10000,FALSE,
ST_SetSRID(ST_MakePoint(127.0728,37.5758),4326)::geography,
ST_SetSRID(ST_MakePoint(127.0658,37.6720),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.0728,37.5758),ST_MakePoint(127.0718,37.5920),ST_MakePoint(127.0705,37.6080),ST_MakePoint(127.0688,37.6240),ST_MakePoint(127.0672,37.6400),ST_MakePoint(127.0658,37.6560),ST_MakePoint(127.0658,37.6720)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000014-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.0728,37.5758),4326)::geography),
(gen_random_uuid(),'c0000014-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.0718,37.5920),4326)::geography),
(gen_random_uuid(),'c0000014-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.0705,37.6080),4326)::geography),
(gen_random_uuid(),'c0000014-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.0688,37.6240),4326)::geography),
(gen_random_uuid(),'c0000014-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(127.0658,37.6720),4326)::geography)
ON CONFLICT DO NOTHING;

-- 15. 경복궁-광화문 코스
INSERT INTO courses VALUES ('c0000015-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'경복궁·광화문 코스','광화문광장에서 출발해 경복궁과 청와대 앞길을 달리는 역사 문화 코스. 국내외 러너들에게 인기 있는 대표 관광 러닝 코스입니다.',
'published',5500,TRUE,
ST_SetSRID(ST_MakePoint(126.9770,37.5796),4326)::geography,
ST_SetSRID(ST_MakePoint(126.9770,37.5796),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.9770,37.5796),ST_MakePoint(126.9745,37.5825),ST_MakePoint(126.9748,37.5892),ST_MakePoint(126.9790,37.5924),ST_MakePoint(126.9842,37.5908),ST_MakePoint(126.9840,37.5865),ST_MakePoint(126.9810,37.5830),ST_MakePoint(126.9800,37.5796),ST_MakePoint(126.9770,37.5796)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000015-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.9770,37.5796),4326)::geography),
(gen_random_uuid(),'c0000015-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(126.9748,37.5892),4326)::geography),
(gen_random_uuid(),'c0000015-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(126.9842,37.5908),4326)::geography),
(gen_random_uuid(),'c0000015-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(126.9840,37.5865),4326)::geography),
(gen_random_uuid(),'c0000015-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(126.9770,37.5796),4326)::geography)
ON CONFLICT DO NOTHING;

-- ===========================================================
-- 부산 (4개)
-- ===========================================================

-- 16. 광안리 해변
INSERT INTO courses VALUES ('c0000016-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'부산 광안리 해변 코스','광안리 해수욕장 해변을 따라 달리는 부산 대표 코스. 광안대교의 웅장한 모습과 바다 뷰가 압권입니다.',
'published',5800,FALSE,
ST_SetSRID(ST_MakePoint(129.1155,35.1538),4326)::geography,
ST_SetSRID(ST_MakePoint(129.1438,35.1498),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(129.1155,35.1538),ST_MakePoint(129.1250,35.1550),ST_MakePoint(129.1350,35.1548),ST_MakePoint(129.1438,35.1498)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000016-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(129.1155,35.1538),4326)::geography),
(gen_random_uuid(),'c0000016-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(129.1250,35.1550),4326)::geography),
(gen_random_uuid(),'c0000016-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(129.1350,35.1548),4326)::geography),
(gen_random_uuid(),'c0000016-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(129.1438,35.1498),4326)::geography)
ON CONFLICT DO NOTHING;

-- 17. 해운대 해변
INSERT INTO courses VALUES ('c0000017-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'부산 해운대 해변 코스','해운대 해수욕장 백사장과 마린시티를 잇는 코스. 동백섬 일주를 포함하면 난이도가 올라갑니다.',
'published',3800,TRUE,
ST_SetSRID(ST_MakePoint(129.1604,35.1587),4326)::geography,
ST_SetSRID(ST_MakePoint(129.1604,35.1587),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(129.1604,35.1587),ST_MakePoint(129.1652,35.1575),ST_MakePoint(129.1718,35.1578),ST_MakePoint(129.1755,35.1602),ST_MakePoint(129.1718,35.1625),ST_MakePoint(129.1652,35.1620),ST_MakePoint(129.1604,35.1587)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000017-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(129.1604,35.1587),4326)::geography),
(gen_random_uuid(),'c0000017-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(129.1718,35.1578),4326)::geography),
(gen_random_uuid(),'c0000017-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(129.1755,35.1602),4326)::geography),
(gen_random_uuid(),'c0000017-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(129.1604,35.1587),4326)::geography)
ON CONFLICT DO NOTHING;

-- 18. 이기대 해안
INSERT INTO courses VALUES ('c0000018-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'부산 이기대 해안 코스','오륙도 스카이워크 인근 이기대 해안을 따라 달리는 코스. 남해 절경과 기암절벽이 러닝을 특별하게 만들어줍니다.',
'published',6200,FALSE,
ST_SetSRID(ST_MakePoint(129.1285,35.1237),4326)::geography,
ST_SetSRID(ST_MakePoint(129.1058,35.1362),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(129.1285,35.1237),ST_MakePoint(129.1215,35.1268),ST_MakePoint(129.1148,35.1308),ST_MakePoint(129.1088,35.1338),ST_MakePoint(129.1058,35.1362)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000018-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(129.1285,35.1237),4326)::geography),
(gen_random_uuid(),'c0000018-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(129.1215,35.1268),4326)::geography),
(gen_random_uuid(),'c0000018-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(129.1148,35.1308),4326)::geography),
(gen_random_uuid(),'c0000018-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(129.1058,35.1362),4326)::geography)
ON CONFLICT DO NOTHING;

-- 19. 수영강 갈맷길
INSERT INTO courses VALUES ('c0000019-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'부산 수영강 갈맷길','수영강을 따라 달리는 부산 갈맷길 코스. 도심과 강변이 조화를 이루며 다양한 거리로 즐길 수 있습니다.',
'published',8500,FALSE,
ST_SetSRID(ST_MakePoint(129.0978,35.1733),4326)::geography,
ST_SetSRID(ST_MakePoint(129.1235,35.1238),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(129.0978,35.1733),ST_MakePoint(129.1020,35.1640),ST_MakePoint(129.1068,35.1545),ST_MakePoint(129.1115,35.1450),ST_MakePoint(129.1165,35.1355),ST_MakePoint(129.1235,35.1238)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000019-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(129.0978,35.1733),4326)::geography),
(gen_random_uuid(),'c0000019-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(129.1020,35.1640),4326)::geography),
(gen_random_uuid(),'c0000019-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(129.1068,35.1545),4326)::geography),
(gen_random_uuid(),'c0000019-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(129.1235,35.1238),4326)::geography)
ON CONFLICT DO NOTHING;

-- ===========================================================
-- 경기도 (5개)
-- ===========================================================

-- 20. 수원화성 성곽길
INSERT INTO courses VALUES ('c0000020-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'수원화성 성곽길','유네스코 세계문화유산 수원화성 성곽을 따라 달리는 역사 코스. 성벽 위를 달리며 수원 도심 전망을 즐깁니다.',
'published',5700,TRUE,
ST_SetSRID(ST_MakePoint(127.0154,37.2880),4326)::geography,
ST_SetSRID(ST_MakePoint(127.0154,37.2880),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.0154,37.2880),ST_MakePoint(127.0218,37.2855),ST_MakePoint(127.0268,37.2812),ST_MakePoint(127.0255,37.2762),ST_MakePoint(127.0192,37.2740),ST_MakePoint(127.0118,37.2762),ST_MakePoint(127.0085,37.2812),ST_MakePoint(127.0102,37.2855),ST_MakePoint(127.0154,37.2880)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000020-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.0154,37.2880),4326)::geography),
(gen_random_uuid(),'c0000020-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.0268,37.2812),4326)::geography),
(gen_random_uuid(),'c0000020-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.0192,37.2740),4326)::geography),
(gen_random_uuid(),'c0000020-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.0085,37.2812),4326)::geography),
(gen_random_uuid(),'c0000020-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(127.0154,37.2880),4326)::geography)
ON CONFLICT DO NOTHING;

-- 21. 탄천 코스 (분당)
INSERT INTO courses VALUES ('c0000021-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'분당 탄천 코스','성남 탄천을 따라 달리는 장거리 코스. 분당 신도시를 관통하며 잘 정비된 자전거·러닝 전용로가 조성되어 있습니다.',
'published',10000,FALSE,
ST_SetSRID(ST_MakePoint(127.1242,37.3812),4326)::geography,
ST_SetSRID(ST_MakePoint(127.1125,37.4760),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.1242,37.3812),ST_MakePoint(127.1215,37.4000),ST_MakePoint(127.1188,37.4180),ST_MakePoint(127.1162,37.4360),ST_MakePoint(127.1142,37.4560),ST_MakePoint(127.1125,37.4760)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000021-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.1242,37.3812),4326)::geography),
(gen_random_uuid(),'c0000021-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.1188,37.4180),4326)::geography),
(gen_random_uuid(),'c0000021-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.1142,37.4560),4326)::geography),
(gen_random_uuid(),'c0000021-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.1125,37.4760),4326)::geography)
ON CONFLICT DO NOTHING;

-- 22. 일산 호수공원
INSERT INTO courses VALUES ('c0000022-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'일산 호수공원 코스','고양 일산 호수공원을 한 바퀴 도는 코스. 국내 최대 인공 호수인 호수공원 주변을 달리며 여유로운 러닝을 즐깁니다.',
'published',4700,TRUE,
ST_SetSRID(ST_MakePoint(126.7625,37.6752),4326)::geography,
ST_SetSRID(ST_MakePoint(126.7625,37.6752),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.7625,37.6752),ST_MakePoint(126.7685,37.6728),ST_MakePoint(126.7728,37.6698),ST_MakePoint(126.7720,37.6662),ST_MakePoint(126.7672,37.6645),ST_MakePoint(126.7622,37.6658),ST_MakePoint(126.7590,37.6692),ST_MakePoint(126.7610,37.6728),ST_MakePoint(126.7625,37.6752)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000022-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.7625,37.6752),4326)::geography),
(gen_random_uuid(),'c0000022-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(126.7728,37.6698),4326)::geography),
(gen_random_uuid(),'c0000022-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(126.7672,37.6645),4326)::geography),
(gen_random_uuid(),'c0000022-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(126.7590,37.6692),4326)::geography),
(gen_random_uuid(),'c0000022-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(126.7625,37.6752),4326)::geography)
ON CONFLICT DO NOTHING;

-- 23. 광교호수공원
INSERT INTO courses VALUES ('c0000023-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'광교호수공원 코스','수원 광교호수공원 원천호와 신대호 주변을 달리는 코스. 카페거리와 조화를 이루는 아름다운 호수 뷰가 인기입니다.',
'published',5200,TRUE,
ST_SetSRID(ST_MakePoint(127.0502,37.2898),4326)::geography,
ST_SetSRID(ST_MakePoint(127.0502,37.2898),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.0502,37.2898),ST_MakePoint(127.0558,37.2878),ST_MakePoint(127.0592,37.2848),ST_MakePoint(127.0578,37.2812),ST_MakePoint(127.0532,37.2798),ST_MakePoint(127.0488,37.2815),ST_MakePoint(127.0465,37.2852),ST_MakePoint(127.0480,37.2882),ST_MakePoint(127.0502,37.2898)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000023-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.0502,37.2898),4326)::geography),
(gen_random_uuid(),'c0000023-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.0558,37.2878),4326)::geography),
(gen_random_uuid(),'c0000023-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.0532,37.2798),4326)::geography),
(gen_random_uuid(),'c0000023-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.0465,37.2852),4326)::geography),
(gen_random_uuid(),'c0000023-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(127.0502,37.2898),4326)::geography)
ON CONFLICT DO NOTHING;

-- 24. 인천 송도 센트럴파크
INSERT INTO courses VALUES ('c0000024-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'인천 송도 센트럴파크 코스','인천 송도국제도시 센트럴파크를 달리는 코스. 바닷물이 흐르는 인공 수로와 현대적인 도시 경관이 특색 있습니다.',
'published',5000,TRUE,
ST_SetSRID(ST_MakePoint(126.6477,37.3929),4326)::geography,
ST_SetSRID(ST_MakePoint(126.6477,37.3929),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.6477,37.3929),ST_MakePoint(126.6535,37.3908),ST_MakePoint(126.6568,37.3878),ST_MakePoint(126.6555,37.3848),ST_MakePoint(126.6508,37.3832),ST_MakePoint(126.6462,37.3848),ST_MakePoint(126.6445,37.3882),ST_MakePoint(126.6455,37.3912),ST_MakePoint(126.6477,37.3929)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000024-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.6477,37.3929),4326)::geography),
(gen_random_uuid(),'c0000024-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(126.6568,37.3878),4326)::geography),
(gen_random_uuid(),'c0000024-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(126.6462,37.3848),4326)::geography),
(gen_random_uuid(),'c0000024-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(126.6477,37.3929),4326)::geography)
ON CONFLICT DO NOTHING;

-- ===========================================================
-- 대전·충청 (3개)
-- ===========================================================

-- 25. 대전 갑천 코스
INSERT INTO courses VALUES ('c0000025-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'대전 갑천 코스','대전 도심을 가로지르는 갑천을 따라 달리는 코스. 엑스포과학공원과 월평공원을 잇는 구간이 특히 인기입니다.',
'published',10000,FALSE,
ST_SetSRID(ST_MakePoint(127.3555,36.3626),4326)::geography,
ST_SetSRID(ST_MakePoint(127.3692,36.4558),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.3555,36.3626),ST_MakePoint(127.3578,36.3800),ST_MakePoint(127.3602,36.3980),ST_MakePoint(127.3628,36.4160),ST_MakePoint(127.3655,36.4340),ST_MakePoint(127.3692,36.4558)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000025-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.3555,36.3626),4326)::geography),
(gen_random_uuid(),'c0000025-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.3602,36.3980),4326)::geography),
(gen_random_uuid(),'c0000025-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.3655,36.4340),4326)::geography),
(gen_random_uuid(),'c0000025-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.3692,36.4558),4326)::geography)
ON CONFLICT DO NOTHING;

-- 26. 청주 무심천 코스
INSERT INTO courses VALUES ('c0000026-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'청주 무심천 코스','청주 도심 무심천을 따라 달리는 강변 코스. 잘 정비된 산책로와 자전거 도로가 러너들에게 쾌적한 환경을 제공합니다.',
'published',10000,FALSE,
ST_SetSRID(ST_MakePoint(127.4865,36.6327),4326)::geography,
ST_SetSRID(ST_MakePoint(127.4752,36.7232),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.4865,36.6327),ST_MakePoint(127.4840,36.6520),ST_MakePoint(127.4815,36.6712),ST_MakePoint(127.4788,36.6908),ST_MakePoint(127.4752,36.7232)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000026-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.4865,36.6327),4326)::geography),
(gen_random_uuid(),'c0000026-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.4815,36.6712),4326)::geography),
(gen_random_uuid(),'c0000026-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.4752,36.7232),4326)::geography)
ON CONFLICT DO NOTHING;

-- 27. 공주 금강 코스
INSERT INTO courses VALUES ('c0000027-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'공주 금강 코스','백제 고도 공주를 가로지르는 금강을 따라 달리는 코스. 공산성과 금강철교를 바라보며 달리는 역사 러닝 코스입니다.',
'published',8000,FALSE,
ST_SetSRID(ST_MakePoint(127.1243,36.4519),4326)::geography,
ST_SetSRID(ST_MakePoint(127.0435,36.4382),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.1243,36.4519),ST_MakePoint(127.1045,36.4488),ST_MakePoint(127.0845,36.4458),ST_MakePoint(127.0640,36.4418),ST_MakePoint(127.0435,36.4382)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000027-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.1243,36.4519),4326)::geography),
(gen_random_uuid(),'c0000027-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.0845,36.4458),4326)::geography),
(gen_random_uuid(),'c0000027-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.0435,36.4382),4326)::geography)
ON CONFLICT DO NOTHING;

-- ===========================================================
-- 광주·전라 (3개)
-- ===========================================================

-- 28. 광주 영산강 코스
INSERT INTO courses VALUES ('c0000028-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'광주 영산강 코스','광주 도심 영산강 자전거길을 따라 달리는 코스. 광주호와 경양방죽 인근 자연 경관을 즐기며 달릴 수 있습니다.',
'published',8000,FALSE,
ST_SetSRID(ST_MakePoint(126.8785,35.1502),4326)::geography,
ST_SetSRID(ST_MakePoint(126.8072,35.1958),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.8785,35.1502),ST_MakePoint(126.8628,35.1618),ST_MakePoint(126.8458,35.1738),ST_MakePoint(126.8268,35.1852),ST_MakePoint(126.8072,35.1958)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000028-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.8785,35.1502),4326)::geography),
(gen_random_uuid(),'c0000028-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(126.8458,35.1738),4326)::geography),
(gen_random_uuid(),'c0000028-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(126.8072,35.1958),4326)::geography)
ON CONFLICT DO NOTHING;

-- 29. 순천만 국가정원 코스
INSERT INTO courses VALUES ('c0000029-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'순천만 국가정원 코스','국내 최초 국가 정원인 순천만 국가정원을 달리는 코스. 갈대밭과 습지를 배경으로 달리는 생태 러닝 코스입니다.',
'published',6000,TRUE,
ST_SetSRID(ST_MakePoint(127.4813,34.9146),4326)::geography,
ST_SetSRID(ST_MakePoint(127.4813,34.9146),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.4813,34.9146),ST_MakePoint(127.4868,34.9118),ST_MakePoint(127.4912,34.9088),ST_MakePoint(127.4905,34.9052),ST_MakePoint(127.4858,34.9035),ST_MakePoint(127.4808,34.9048),ST_MakePoint(127.4772,34.9082),ST_MakePoint(127.4780,34.9118),ST_MakePoint(127.4813,34.9146)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000029-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.4813,34.9146),4326)::geography),
(gen_random_uuid(),'c0000029-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.4912,34.9088),4326)::geography),
(gen_random_uuid(),'c0000029-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.4772,34.9082),4326)::geography),
(gen_random_uuid(),'c0000029-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.4813,34.9146),4326)::geography)
ON CONFLICT DO NOTHING;

-- 30. 여수 오동도 코스
INSERT INTO courses VALUES ('c0000030-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'여수 오동도 코스','여수 오동도 섬을 한 바퀴 도는 코스. 동백꽃과 남해 절경, 여수항 야경을 배경으로 달립니다.',
'published',3200,TRUE,
ST_SetSRID(ST_MakePoint(127.7470,34.7416),4326)::geography,
ST_SetSRID(ST_MakePoint(127.7470,34.7416),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.7470,34.7416),ST_MakePoint(127.7508,34.7402),ST_MakePoint(127.7542,34.7385),ST_MakePoint(127.7545,34.7358),ST_MakePoint(127.7520,34.7338),ST_MakePoint(127.7482,34.7340),ST_MakePoint(127.7452,34.7360),ST_MakePoint(127.7448,34.7390),ST_MakePoint(127.7470,34.7416)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000030-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.7470,34.7416),4326)::geography),
(gen_random_uuid(),'c0000030-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.7542,34.7385),4326)::geography),
(gen_random_uuid(),'c0000030-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.7520,34.7338),4326)::geography),
(gen_random_uuid(),'c0000030-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.7452,34.7360),4326)::geography),
(gen_random_uuid(),'c0000030-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(127.7470,34.7416),4326)::geography)
ON CONFLICT DO NOTHING;

-- ===========================================================
-- 대구·경상 (3개)
-- ===========================================================

-- 31. 대구 수성못 코스
INSERT INTO courses VALUES ('c0000031-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'대구 수성못 코스','대구 수성구의 명소 수성못을 한 바퀴 도는 코스. 호수 주변 아름다운 산책로와 야경으로 저녁 러닝 명소입니다.',
'published',3500,TRUE,
ST_SetSRID(ST_MakePoint(128.6478,35.8453),4326)::geography,
ST_SetSRID(ST_MakePoint(128.6478,35.8453),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(128.6478,35.8453),ST_MakePoint(128.6518,35.8435),ST_MakePoint(128.6542,35.8408),ST_MakePoint(128.6535,35.8378),ST_MakePoint(128.6498,35.8362),ST_MakePoint(128.6458,35.8370),ST_MakePoint(128.6435,35.8398),ST_MakePoint(128.6442,35.8430),ST_MakePoint(128.6478,35.8453)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000031-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(128.6478,35.8453),4326)::geography),
(gen_random_uuid(),'c0000031-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(128.6542,35.8408),4326)::geography),
(gen_random_uuid(),'c0000031-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(128.6435,35.8398),4326)::geography),
(gen_random_uuid(),'c0000031-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(128.6478,35.8453),4326)::geography)
ON CONFLICT DO NOTHING;

-- 32. 울산 태화강 국가정원
INSERT INTO courses VALUES ('c0000032-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'울산 태화강 국가정원 코스','울산 태화강 국가정원을 달리는 코스. 십리대숲을 통과하며 달리는 구간이 특히 인상적입니다. 봄 유채꽃 시즌에 절경을 이룹니다.',
'published',8000,TRUE,
ST_SetSRID(ST_MakePoint(129.3164,35.5411),4326)::geography,
ST_SetSRID(ST_MakePoint(129.3164,35.5411),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(129.3164,35.5411),ST_MakePoint(129.3248,35.5388),ST_MakePoint(129.3312,35.5358),ST_MakePoint(129.3318,35.5322),ST_MakePoint(129.3270,35.5298),ST_MakePoint(129.3192,35.5308),ST_MakePoint(129.3125,35.5338),ST_MakePoint(129.3105,35.5372),ST_MakePoint(129.3140,35.5400),ST_MakePoint(129.3164,35.5411)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000032-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(129.3164,35.5411),4326)::geography),
(gen_random_uuid(),'c0000032-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(129.3312,35.5358),4326)::geography),
(gen_random_uuid(),'c0000032-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(129.3192,35.5308),4326)::geography),
(gen_random_uuid(),'c0000032-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(129.3105,35.5372),4326)::geography),
(gen_random_uuid(),'c0000032-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(129.3164,35.5411),4326)::geography)
ON CONFLICT DO NOTHING;

-- 33. 창원 용지호수 코스
INSERT INTO courses VALUES ('c0000033-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'창원 용지호수 코스','창원 도심 용지호수 주변을 달리는 코스. 용지공원 산책로가 잘 조성되어 있으며 계절마다 다른 꽃을 즐길 수 있습니다.',
'published',4000,TRUE,
ST_SetSRID(ST_MakePoint(128.6812,35.2285),4326)::geography,
ST_SetSRID(ST_MakePoint(128.6812,35.2285),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(128.6812,35.2285),ST_MakePoint(128.6858,35.2262),ST_MakePoint(128.6882,35.2232),ST_MakePoint(128.6868,35.2200),ST_MakePoint(128.6825,35.2185),ST_MakePoint(128.6778,35.2198),ST_MakePoint(128.6758,35.2232),ST_MakePoint(128.6775,35.2262),ST_MakePoint(128.6812,35.2285)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000033-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(128.6812,35.2285),4326)::geography),
(gen_random_uuid(),'c0000033-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(128.6882,35.2232),4326)::geography),
(gen_random_uuid(),'c0000033-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(128.6758,35.2232),4326)::geography),
(gen_random_uuid(),'c0000033-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(128.6812,35.2285),4326)::geography)
ON CONFLICT DO NOTHING;

-- ===========================================================
-- 강원도 (3개)
-- ===========================================================

-- 34. 강릉 경포 코스
INSERT INTO courses VALUES ('c0000034-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'강릉 경포 코스','경포해변과 경포호를 잇는 강릉 대표 러닝 코스. 동해 바다와 호수를 동시에 즐기며 달릴 수 있습니다.',
'published',5000,FALSE,
ST_SetSRID(ST_MakePoint(128.9041,37.7962),4326)::geography,
ST_SetSRID(ST_MakePoint(128.8852,37.7868),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(128.9041,37.7962),ST_MakePoint(128.8998,37.7952),ST_MakePoint(128.8952,37.7938),ST_MakePoint(128.8908,37.7912),ST_MakePoint(128.8872,37.7888),ST_MakePoint(128.8852,37.7868)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000034-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(128.9041,37.7962),4326)::geography),
(gen_random_uuid(),'c0000034-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(128.8952,37.7938),4326)::geography),
(gen_random_uuid(),'c0000034-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(128.8852,37.7868),4326)::geography)
ON CONFLICT DO NOTHING;

-- 35. 춘천 의암호 코스
INSERT INTO courses VALUES ('c0000035-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'춘천 의암호 코스','춘천 의암호 주변을 달리는 코스. 드라마 촬영지로 유명한 낭만적인 호반 도시 춘천의 아름다운 뷰를 즐깁니다.',
'published',8000,FALSE,
ST_SetSRID(ST_MakePoint(127.7215,37.8862),4326)::geography,
ST_SetSRID(ST_MakePoint(127.6788,37.9342),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.7215,37.8862),ST_MakePoint(127.7105,37.8988),ST_MakePoint(127.6998,37.9115),ST_MakePoint(127.6888,37.9228),ST_MakePoint(127.6788,37.9342)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000035-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.7215,37.8862),4326)::geography),
(gen_random_uuid(),'c0000035-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.6998,37.9115),4326)::geography),
(gen_random_uuid(),'c0000035-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.6788,37.9342),4326)::geography)
ON CONFLICT DO NOTHING;

-- 36. 속초 청초호·설악 코스
INSERT INTO courses VALUES ('c0000036-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'속초 청초호 코스','속초 청초호를 한 바퀴 도는 코스. 설악산을 배경으로 달리며 속초항과 아바이마을의 풍경을 즐깁니다.',
'published',4500,TRUE,
ST_SetSRID(ST_MakePoint(128.5912,38.2042),4326)::geography,
ST_SetSRID(ST_MakePoint(128.5912,38.2042),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(128.5912,38.2042),ST_MakePoint(128.5965,38.2018),ST_MakePoint(128.5998,38.1985),ST_MakePoint(128.5985,38.1952),ST_MakePoint(128.5942,38.1938),ST_MakePoint(128.5895,38.1952),ST_MakePoint(128.5872,38.1988),ST_MakePoint(128.5885,38.2022),ST_MakePoint(128.5912,38.2042)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000036-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(128.5912,38.2042),4326)::geography),
(gen_random_uuid(),'c0000036-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(128.5998,38.1985),4326)::geography),
(gen_random_uuid(),'c0000036-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(128.5872,38.1988),4326)::geography),
(gen_random_uuid(),'c0000036-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(128.5912,38.2042),4326)::geography)
ON CONFLICT DO NOTHING;

-- ===========================================================
-- 제주도 (3개)
-- ===========================================================

-- 37. 제주 올레 1코스
INSERT INTO courses VALUES ('c0000037-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'제주 올레 1코스','시흥초등학교에서 광치기해변까지 이어지는 제주올레 1코스. 성산일출봉과 바닷가 절경을 함께 즐기는 국내 최고 트레일런 코스입니다.',
'published',15000,FALSE,
ST_SetSRID(ST_MakePoint(126.9297,33.4627),4326)::geography,
ST_SetSRID(ST_MakePoint(126.9282,33.4522),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.9297,33.4627),ST_MakePoint(126.9315,33.4590),ST_MakePoint(126.9350,33.4560),ST_MakePoint(126.9382,33.4542),ST_MakePoint(126.9355,33.4518),ST_MakePoint(126.9318,33.4510),ST_MakePoint(126.9282,33.4522)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000037-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.9297,33.4627),4326)::geography),
(gen_random_uuid(),'c0000037-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(126.9350,33.4560),4326)::geography),
(gen_random_uuid(),'c0000037-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(126.9382,33.4542),4326)::geography),
(gen_random_uuid(),'c0000037-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(126.9282,33.4522),4326)::geography)
ON CONFLICT DO NOTHING;

-- 38. 제주 한라산 어리목 코스
INSERT INTO courses VALUES ('c0000038-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'제주 한라산 어리목 코스','한라산 어리목 탐방로를 달리는 코스. 해발 970m 윗세오름까지 오르는 도전적인 트레일런 코스입니다.',
'published',9600,FALSE,
ST_SetSRID(ST_MakePoint(126.5067,33.3817),4326)::geography,
ST_SetSRID(ST_MakePoint(126.5285,33.3612),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.5067,33.3817),ST_MakePoint(126.5112,33.3775),ST_MakePoint(126.5162,33.3732),ST_MakePoint(126.5218,33.3688),ST_MakePoint(126.5258,33.3645),ST_MakePoint(126.5285,33.3612)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000038-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.5067,33.3817),4326)::geography),
(gen_random_uuid(),'c0000038-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(126.5162,33.3732),4326)::geography),
(gen_random_uuid(),'c0000038-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(126.5258,33.3645),4326)::geography),
(gen_random_uuid(),'c0000038-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(126.5285,33.3612),4326)::geography)
ON CONFLICT DO NOTHING;

-- 39. 제주 함덕해변 코스
INSERT INTO courses VALUES ('c0000039-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'제주 함덕해변 코스','제주 북쪽 함덕 서우봉 해변을 달리는 코스. 에메랄드빛 바다와 서우봉 능선을 배경으로 달릴 수 있습니다.',
'published',5500,TRUE,
ST_SetSRID(ST_MakePoint(126.6693,33.5437),4326)::geography,
ST_SetSRID(ST_MakePoint(126.6693,33.5437),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.6693,33.5437),ST_MakePoint(126.6748,33.5422),ST_MakePoint(126.6798,33.5405),ST_MakePoint(126.6838,33.5378),ST_MakePoint(126.6828,33.5348),ST_MakePoint(126.6785,33.5338),ST_MakePoint(126.6735,33.5348),ST_MakePoint(126.6698,33.5372),ST_MakePoint(126.6678,33.5405),ST_MakePoint(126.6693,33.5437)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000039-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.6693,33.5437),4326)::geography),
(gen_random_uuid(),'c0000039-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(126.6838,33.5378),4326)::geography),
(gen_random_uuid(),'c0000039-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(126.6678,33.5405),4326)::geography),
(gen_random_uuid(),'c0000039-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(126.6693,33.5437),4326)::geography)
ON CONFLICT DO NOTHING;

-- ===========================================================
-- 추가 (서울·경기 보완, 총 33개 초과)
-- ===========================================================

-- 40. 한강 마포-합정 코스
INSERT INTO courses VALUES ('c0000040-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'한강 마포·합정 코스','마포한강공원에서 합정까지 달리는 코스. 한강 다리 뷰와 서울 야경이 아름다우며 접근성이 뛰어납니다.',
'published',5000,FALSE,
ST_SetSRID(ST_MakePoint(126.9488,37.5488),4326)::geography,
ST_SetSRID(ST_MakePoint(126.9000,37.5512),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(126.9488,37.5488),ST_MakePoint(126.9378,37.5498),ST_MakePoint(126.9248,37.5505),ST_MakePoint(126.9125,37.5510),ST_MakePoint(126.9000,37.5512)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000040-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(126.9488,37.5488),4326)::geography),
(gen_random_uuid(),'c0000040-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(126.9248,37.5505),4326)::geography),
(gen_random_uuid(),'c0000040-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(126.9000,37.5512),4326)::geography)
ON CONFLICT DO NOTHING;

-- 41. 성남 분당중앙공원 코스
INSERT INTO courses VALUES ('c0000041-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'성남 중앙공원 코스','성남 분당 중앙공원을 달리는 코스. 넓은 잔디밭과 호수, 분수대를 포함한 쾌적한 러닝 환경을 제공합니다.',
'published',3800,TRUE,
ST_SetSRID(ST_MakePoint(127.1115,37.3842),4326)::geography,
ST_SetSRID(ST_MakePoint(127.1115,37.3842),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.1115,37.3842),ST_MakePoint(127.1158,37.3822),ST_MakePoint(127.1182,37.3795),ST_MakePoint(127.1170,37.3768),ST_MakePoint(127.1130,37.3755),ST_MakePoint(127.1090,37.3768),ST_MakePoint(127.1072,37.3795),ST_MakePoint(127.1082,37.3822),ST_MakePoint(127.1115,37.3842)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000041-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.1115,37.3842),4326)::geography),
(gen_random_uuid(),'c0000041-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.1182,37.3795),4326)::geography),
(gen_random_uuid(),'c0000041-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.1072,37.3795),4326)::geography),
(gen_random_uuid(),'c0000041-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(127.1115,37.3842),4326)::geography)
ON CONFLICT DO NOTHING;

-- 42. 전주 전주천 코스
INSERT INTO courses VALUES ('c0000042-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'전주 전주천 코스','전주천을 따라 달리는 코스. 한옥마을 인근을 지나며 전통 도시 전주의 정취를 느낄 수 있습니다.',
'published',8000,FALSE,
ST_SetSRID(ST_MakePoint(127.1478,35.8225),4326)::geography,
ST_SetSRID(ST_MakePoint(127.1248,35.8912),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(127.1478,35.8225),ST_MakePoint(127.1428,35.8392),ST_MakePoint(127.1378,35.8558),ST_MakePoint(127.1320,35.8725),ST_MakePoint(127.1248,35.8912)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000042-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(127.1478,35.8225),4326)::geography),
(gen_random_uuid(),'c0000042-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(127.1378,35.8558),4326)::geography),
(gen_random_uuid(),'c0000042-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(127.1248,35.8912),4326)::geography)
ON CONFLICT DO NOTHING;

-- 43. 경주 대릉원·첨성대 코스
INSERT INTO courses VALUES ('c0000043-0000-0000-0000-000000000000','00000000-0000-0000-0000-000000000001',NULL,
'경주 대릉원·첨성대 코스','신라 천년의 역사를 품은 경주 대릉원과 첨성대, 안압지를 잇는 역사 러닝 코스. 고분군과 문화재를 곁에 두고 달립니다.',
'published',6500,TRUE,
ST_SetSRID(ST_MakePoint(129.2195,35.8362),4326)::geography,
ST_SetSRID(ST_MakePoint(129.2195,35.8362),4326)::geography,
ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(129.2195,35.8362),ST_MakePoint(129.2242,35.8342),ST_MakePoint(129.2282,35.8315),ST_MakePoint(129.2298,35.8282),ST_MakePoint(129.2278,35.8255),ST_MakePoint(129.2238,35.8242),ST_MakePoint(129.2195,35.8255),ST_MakePoint(129.2170,35.8285),ST_MakePoint(129.2178,35.8325),ST_MakePoint(129.2195,35.8362)]),4326)::geography,
0,0,NOW(),NOW(),NULL) ON CONFLICT (id) DO NOTHING;

INSERT INTO course_points(id,course_id,sequence,location) VALUES
(gen_random_uuid(),'c0000043-0000-0000-0000-000000000000',1,ST_SetSRID(ST_MakePoint(129.2195,35.8362),4326)::geography),
(gen_random_uuid(),'c0000043-0000-0000-0000-000000000000',2,ST_SetSRID(ST_MakePoint(129.2282,35.8315),4326)::geography),
(gen_random_uuid(),'c0000043-0000-0000-0000-000000000000',3,ST_SetSRID(ST_MakePoint(129.2238,35.8242),4326)::geography),
(gen_random_uuid(),'c0000043-0000-0000-0000-000000000000',4,ST_SetSRID(ST_MakePoint(129.2170,35.8285),4326)::geography),
(gen_random_uuid(),'c0000043-0000-0000-0000-000000000000',5,ST_SetSRID(ST_MakePoint(129.2195,35.8362),4326)::geography)
ON CONFLICT DO NOTHING;
