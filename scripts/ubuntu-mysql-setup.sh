#!/usr/bin/env bash
# Ubuntu(WSL) MySQL 설치 + CX 트래커 DB/계정 생성
# 사용: bash /mnt/d/ispark_prj/flowdesk/scripts/ubuntu-mysql-setup.sh
# (sudo 비밀번호 1회 입력)

set -euo pipefail

DB_NAME="cx_tracker"
DB_USER="${CX_DB_USER:-cx}"
DB_PASS="${CX_DB_PASSWORD:-}"
if [ -z "$DB_PASS" ]; then
  echo "CX_DB_PASSWORD 환경변수를 설정한 뒤 다시 실행하세요."
  echo "예: export CX_DB_PASSWORD='your-password'"
  exit 1
fi

echo "=== 1) MySQL 설치 ==="
sudo apt-get update
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y mysql-server mysql-client

echo "=== 2) MySQL 기동 ==="
sudo service mysql start
sleep 2
sudo service mysql status | head -10 || true

echo "=== 3) DB / 사용자 생성 ==="
# root는 소켓 인증(sudo mysql)으로 접속
sudo mysql <<SQL
CREATE DATABASE IF NOT EXISTS ${DB_NAME}
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASS}';

GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'%';
FLUSH PRIVILEGES;
SQL

echo "=== 4) 테이블 생성 ==="
SCHEMA="/mnt/d/ispark_prj/flowdesk/src/main/resources/schema.sql"
if [[ ! -f "$SCHEMA" ]]; then
  SCHEMA="$(dirname "$0")/../src/main/resources/schema.sql"
fi

mysql -u"${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" < "$SCHEMA"

echo "=== 5) 연결 테스트 ==="
mysql -u"${DB_USER}" -p"${DB_PASS}" -e "USE ${DB_NAME}; SHOW TABLES; SELECT COUNT(*) AS cnt FROM inquiries;"

echo ""
echo "완료!"
echo "  DB   : ${DB_NAME}"
echo "  USER : ${DB_USER}"
echo "  PASS : ${DB_PASS}"
echo "  URL  : jdbc:mysql://127.0.0.1:3306/${DB_NAME}"
echo ""
echo "다음: Tomcat에 MySQL 프로필 적용"
echo "  cp /mnt/d/ispark_prj/flowdesk/scripts/tomcat-setenv-mysql.sh ~/tomcat/bin/setenv.sh"
echo "  chmod +x ~/tomcat/bin/setenv.sh"
echo "  ~/tomcat/bin/shutdown.sh; ~/tomcat/bin/startup.sh"
echo ""
echo "비밀번호를 바꿨다면 application-mysql.yml 또는 환경변수 CX_DB_PASSWORD 도 맞추세요."
