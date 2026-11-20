-- Transform 다중 원본 경로 지원: 기존 버전별 필드 데이터를 유지한다.
-- 적용 전 백업 및 사전 테스트 필수. PostgreSQL 트랜잭션 내에서 실행한다.
BEGIN;

-- 1. 기존 VARCHAR 경로의 비어 있지 않음 검사 제거 (JSONB 변환 전에 필요).
ALTER TABLE sched_transform_rule_field
    DROP CONSTRAINT ck_transform_rule_field_source_path;

-- 2. 기존 컬럼을 이름 변경하고 모든 경로를 JSONB 배열로 변환한다.
-- 예: 'dealYear' -> '["dealYear"]'
ALTER TABLE sched_transform_rule_field
    RENAME COLUMN source_path TO source_paths;

ALTER TABLE sched_transform_rule_field
    ALTER COLUMN source_paths TYPE JSONB
    USING jsonb_build_array(source_paths);

-- 3. 기존 규칙은 DIRECT로 유지한다.
ALTER TABLE sched_transform_rule_field
    ADD COLUMN transform_type VARCHAR(30) NOT NULL DEFAULT 'DIRECT',
    ADD COLUMN separator VARCHAR(20);

-- 4. 경로 목록과 결합 유형을 검증한다.
ALTER TABLE sched_transform_rule_field
    ADD CONSTRAINT ck_transform_rule_field_source_paths
    CHECK (
        jsonb_typeof(source_paths) = 'array'
        AND jsonb_array_length(source_paths) > 0
    ),
    ADD CONSTRAINT ck_transform_rule_field_transform_type
    CHECK (transform_type IN ('DIRECT', 'CONCAT', 'DATE_COMBINE')),
    ADD CONSTRAINT ck_transform_rule_field_transform_input
    CHECK (
        (transform_type = 'DIRECT' AND jsonb_array_length(source_paths) = 1)
        OR (transform_type = 'CONCAT' AND jsonb_array_length(source_paths) >= 2
            AND data_type = 'STRING' AND separator IS NOT NULL)
        OR (transform_type = 'DATE_COMBINE' AND jsonb_array_length(source_paths) = 3
            AND data_type = 'STRING' AND separator IS NULL)
    );

COMMENT ON COLUMN sched_transform_rule_field.source_paths
IS '원본 데이터 경로 배열. DIRECT 1개, CONCAT 2개 이상, DATE_COMBINE 연·월·일 3개';
COMMENT ON COLUMN sched_transform_rule_field.transform_type
IS '가공 방식(DIRECT, CONCAT, DATE_COMBINE)';
COMMENT ON COLUMN sched_transform_rule_field.separator
IS 'CONCAT 원본 필드 간 구분자(빈 문자열 허용)';

COMMIT;

-- 마이그레이션 후 검증
SELECT transform_type,
       COUNT(*) AS field_count,
       MIN(jsonb_array_length(source_paths)) AS min_path_count,
       MAX(jsonb_array_length(source_paths)) AS max_path_count
  FROM sched_transform_rule_field
 GROUP BY transform_type;
