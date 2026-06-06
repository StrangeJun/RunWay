-- V13: 시드 코스 경로 좌표 촘촘히 보완
--
-- V12 시드 코스는 3~9개의 anchor 좌표만 갖고 있어
-- 지도에서 직선 다각형처럼 보인다.
-- ST_Segmentize로 경로를 100m 간격으로 분할해
-- course_points를 재생성하면 충주처럼 부드러운 경로가 표시된다.
--
-- 적용 대상: creator_id = '00000000-...-000001' (RunWay 공식 시스템 계정)

DO $$
DECLARE
    rec      RECORD;
    dense    geography;
    geom     geometry;
    n_pts    INT;
    i        INT;
BEGIN
    FOR rec IN
        SELECT id FROM courses
        WHERE creator_id = '00000000-0000-0000-0000-000000000001'
          AND path IS NOT NULL
    LOOP
        -- 100 m 간격으로 중간 좌표 삽입 (geography 단위: 미터)
        SELECT ST_Segmentize(path, 100) INTO dense
        FROM courses WHERE id = rec.id;

        IF dense IS NULL THEN
            CONTINUE;
        END IF;

        -- courses.path 업데이트
        UPDATE courses SET path = dense WHERE id = rec.id;

        -- 기존 포인트 삭제 후 재삽입
        DELETE FROM course_points WHERE course_id = rec.id;

        geom  := dense::geometry;
        n_pts := ST_NPoints(geom);

        FOR i IN 1..n_pts LOOP
            INSERT INTO course_points (id, course_id, sequence, location)
            VALUES (
                gen_random_uuid(),
                rec.id,
                i,
                ST_SetSRID(ST_PointN(geom, i), 4326)::geography
            );
        END LOOP;

    END LOOP;
END $$;
