-- CX 문의관리 트래커 테이블 (H2 / MySQL 공통 문법)
CREATE TABLE IF NOT EXISTS inquiries (
  id                 VARCHAR(64)  NOT NULL PRIMARY KEY,
  direction          VARCHAR(32)  DEFAULT '인바운드',
  channel            VARCHAR(32),
  status             VARCHAR(32),
  category           VARCHAR(64),
  module             VARCHAR(64),
  inbound_at         VARCHAR(32),
  first_response_at  VARCHAR(32),
  resolved_at        VARCHAR(32),
  customer           VARCHAR(200),
  reviewer           VARCHAR(100),
  note               VARCHAR(2000),
  linked_id          VARCHAR(64),
  create_user        VARCHAR(100)
);

-- 인바디(체성분) 측정 기록 — 사용자 개인 소유
CREATE TABLE IF NOT EXISTS inbody_records (
  id                   VARCHAR(64)   NOT NULL PRIMARY KEY,
  measured_at          VARCHAR(32)   NOT NULL,
  height_cm            DOUBLE,
  age                  INT,
  gender               VARCHAR(8),
  weight_kg            DOUBLE,
  skeletal_muscle_kg   DOUBLE,
  body_fat_mass_kg     DOUBLE,
  total_body_water_kg  DOUBLE,
  protein_kg           DOUBLE,
  mineral_kg           DOUBLE,
  fat_free_mass_kg     DOUBLE,
  bmi                  DOUBLE,
  percent_body_fat     DOUBLE,
  waist_hip_ratio      DOUBLE,
  bmr_kcal             INT,
  visceral_fat_level   INT,
  fitness_score        INT,
  muscle_control_kg    DOUBLE,
  fat_control_kg       DOUBLE,
  note                 VARCHAR(2000),
  create_user          VARCHAR(100),
  create_dt            VARCHAR(32),
  modify_dt            VARCHAR(32)
);

-- 접속 로그 (기기·GPS·시각)
CREATE TABLE IF NOT EXISTS access_logs (
  id              VARCHAR(64)   NOT NULL PRIMARY KEY,
  accessed_at     VARCHAR(32)   NOT NULL,
  username        VARCHAR(100),
  event_type      VARCHAR(32),
  page_path       VARCHAR(200),
  menu_nm         VARCHAR(100),
  device_type     VARCHAR(32),
  user_agent      VARCHAR(1000),
  ip_address      VARCHAR(64),
  latitude        DOUBLE,
  longitude       DOUBLE,
  gps_accuracy_m  DOUBLE,
  gps_status      VARCHAR(64)
);

-- 권한 마스터 (ispark com_code 018 역할)
CREATE TABLE IF NOT EXISTS fd_auth (
  auth_cd    VARCHAR(10)   NOT NULL PRIMARY KEY,
  auth_nm    VARCHAR(100)  NOT NULL,
  sort_order INT           DEFAULT 0,
  use_yn     CHAR(1)       DEFAULT 'Y',
  create_dt  VARCHAR(32),
  note       VARCHAR(500)
);

-- 메뉴 관리 (ispark com_pgm 참고 + 확장 컬럼)
CREATE TABLE IF NOT EXISTS fd_menu (
  menu_id        VARCHAR(64)   NOT NULL PRIMARY KEY,
  menu_key       VARCHAR(64)   NOT NULL,
  menu_nm        VARCHAR(100)  NOT NULL,
  page_url       VARCHAR(200),
  api_prefix     VARCHAR(200),
  up_menu_id     VARCHAR(64),
  menu_level     INT           DEFAULT 1,
  sort_order     INT           DEFAULT 0,
  real_order     INT           DEFAULT 0,
  is_leaf        CHAR(1)       DEFAULT 'Y',
  use_yn         CHAR(1)       DEFAULT 'Y',
  restricted_yn  CHAR(1)       DEFAULT 'N',
  icon           VARCHAR(32),
  link_url       VARCHAR(500),
  new_url        VARCHAR(500),
  path           VARCHAR(500),
  source_type    VARCHAR(8),
  guide_comment  VARCHAR(1000),
  content        VARCHAR(2000),
  param          VARCHAR(500),
  manual_url     VARCHAR(500),
  create_dt      VARCHAR(32),
  update_dt      VARCHAR(32),
  delete_dt      VARCHAR(32),
  note           VARCHAR(500),
  CONSTRAINT uk_fd_menu_key UNIQUE (menu_key)
);

-- 메뉴 ↔ 권한 (ispark com_pgm_auth)
CREATE TABLE IF NOT EXISTS fd_menu_auth (
  auth_cd   VARCHAR(10)  NOT NULL,
  menu_id   VARCHAR(64)  NOT NULL,
  create_dt VARCHAR(32),
  PRIMARY KEY (auth_cd, menu_id)
);

-- 개인 리마인드
CREATE TABLE IF NOT EXISTS reminders (
  id           VARCHAR(64)   NOT NULL PRIMARY KEY,
  username     VARCHAR(100)  NOT NULL,
  remind_date  VARCHAR(10)   NOT NULL,
  title        VARCHAR(200)  NOT NULL,
  content      VARCHAR(2000),
  done_yn      CHAR(1)       DEFAULT 'N',
  create_dt    VARCHAR(32),
  modify_dt    VARCHAR(32)
);

-- 앱 사용자 (회원 등록)
CREATE TABLE IF NOT EXISTS app_users (
  id           VARCHAR(64)   NOT NULL PRIMARY KEY,
  username     VARCHAR(100)  NOT NULL,
  password     VARCHAR(200)  NOT NULL,
  role         VARCHAR(32)   NOT NULL,
  auth_cd      VARCHAR(10),
  display_name VARCHAR(100),
  created_at   VARCHAR(32),
  note         VARCHAR(500),
  profile_image VARCHAR(200),
  CONSTRAINT uk_app_users_username UNIQUE (username)
);

-- 홈 실시간 채팅 메시지
CREATE TABLE IF NOT EXISTS chat_messages (
  id           VARCHAR(64)   NOT NULL PRIMARY KEY,
  username     VARCHAR(100),
  display_name VARCHAR(100),
  body_text    VARCHAR(4000),
  created_at   VARCHAR(32)   NOT NULL
);

-- 채팅 첨부파일
CREATE TABLE IF NOT EXISTS chat_attachments (
  id             VARCHAR(64)   NOT NULL PRIMARY KEY,
  message_id     VARCHAR(64),
  original_name  VARCHAR(300),
  stored_name    VARCHAR(300)  NOT NULL,
  content_type   VARCHAR(120),
  size_bytes     BIGINT,
  uploaded_by    VARCHAR(100),
  uploaded_at    VARCHAR(32)   NOT NULL
);
