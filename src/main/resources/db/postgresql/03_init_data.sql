-- =========================================================
-- Scheduler initial data
-- PostgreSQL
-- =========================================================


-- =========================================================
-- 1. Role
-- =========================================================

INSERT INTO app_role (
    name
)
VALUES (
    'ROLE_ADMIN'
);


-- =========================================================
-- 2. User
-- BCrypt encoded password
-- =========================================================

INSERT INTO app_user (
    username,
    password,
    enabled
)
VALUES (
    'admin1',
    '$2a$10$46ikTYGHM12Sljb2LRWuu.5Hq2SN/ZQAfSY/WBarCQUPX4Rb.g1UW',
    1
);


-- =========================================================
-- 3. User - Role mapping
-- =========================================================

INSERT INTO app_user_role (
    user_id,
    role_id
)
SELECT
    u.id,
    r.id
FROM app_user u
CROSS JOIN app_role r
WHERE u.username = 'admin1'
  AND r.name = 'ROLE_ADMIN';