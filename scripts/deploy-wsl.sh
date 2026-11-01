#!/usr/bin/env bash
# FlowDesk → Tomcat 배포 (WSL2 Ubuntu에서 실행)
# 사용:
#   cd /mnt/d/ispark_prj/flowdesk
#   bash scripts/deploy-wsl.sh
# 옵션 환경변수:
#   FLOWDESK_BRANCH   배포 브랜치 (기본: 현재 브랜치, 없으면 develop)
#   FLOWDESK_TOMCAT   Tomcat 홈 (기본: $HOME/tomcat)
#   FLOWDESK_MVN      mvn 경로
#   SKIP_GIT=1        git pull 생략
#   SKIP_BUILD=1      maven 빌드 생략 (이미 war 있을 때)
set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TOMCAT_HOME="${FLOWDESK_TOMCAT:-$HOME/tomcat}"
MVN_BIN="${FLOWDESK_MVN:-/mnt/d/ispark_prj/tools/apache-maven-3.9.6/bin/mvn}"
HEALTH_URL="${FLOWDESK_HEALTH_URL:-http://127.0.0.1:8080/flowdesk/login.html}"
BRANCH="${FLOWDESK_BRANCH:-}"

cd "$REPO_DIR"
echo "== FlowDesk deploy =="
echo "repo:   $REPO_DIR"
echo "tomcat: $TOMCAT_HOME"

if [ ! -x "$TOMCAT_HOME/bin/catalina.sh" ]; then
  echo "Tomcat을 찾을 수 없습니다: $TOMCAT_HOME"
  exit 1
fi

if [ ! -x "$MVN_BIN" ]; then
  if command -v mvn >/dev/null 2>&1; then
    MVN_BIN="$(command -v mvn)"
  else
    echo "Maven을 찾을 수 없습니다. FLOWDESK_MVN 또는 tools/apache-maven 경로를 확인하세요."
    exit 1
  fi
fi

# --- git ---
if [ "${SKIP_GIT:-0}" != "1" ]; then
  if [ -z "$BRANCH" ]; then
    BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || true)"
  fi
  if [ -z "$BRANCH" ] || [ "$BRANCH" = "HEAD" ]; then
    BRANCH="develop"
  fi
  echo "git: fetch/checkout/pull ($BRANCH)"
  git fetch origin
  git checkout "$BRANCH"
  git pull --ff-only origin "$BRANCH"
else
  echo "git: skipped"
fi

# --- build ---
if [ "${SKIP_BUILD:-0}" != "1" ]; then
  echo "maven: package"
  "$MVN_BIN" -B -DskipTests package
else
  echo "maven: skipped"
fi

WAR="$REPO_DIR/target/flowdesk.war"
if [ ! -f "$WAR" ]; then
  echo "WAR 없음: $WAR"
  exit 1
fi

# --- stop tomcat ---
echo "tomcat: shutdown"
"$TOMCAT_HOME/bin/shutdown.sh" >/dev/null 2>&1 || true
sleep 3
# 남아 있으면 강제 종료 (WSL에서 가끔 잔류)
if pgrep -f "catalina.base=$TOMCAT_HOME" >/dev/null 2>&1 || pgrep -f "org.apache.catalina.startup.Bootstrap" >/dev/null 2>&1; then
  echo "tomcat: leftover java → kill"
  pkill -f "org.apache.catalina.startup.Bootstrap" >/dev/null 2>&1 || true
  sleep 2
fi

# --- deploy war ---
echo "deploy: copy WAR"
rm -rf "$TOMCAT_HOME/webapps/flowdesk" "$TOMCAT_HOME/webapps/flowdesk.war"
cp "$WAR" "$TOMCAT_HOME/webapps/flowdesk.war"

# setenv 안내 (비밀번호는 로컬 setenv에만)
if [ ! -f "$TOMCAT_HOME/bin/setenv.sh" ]; then
  echo "경고: $TOMCAT_HOME/bin/setenv.sh 없음. mysql 프로필/업로드 경로가 빠질 수 있습니다."
  echo "  예: cp scripts/tomcat-setenv-mysql.sh.example $TOMCAT_HOME/bin/setenv.sh"
fi

# --- start ---
echo "tomcat: start"
nohup "$TOMCAT_HOME/bin/catalina.sh" start >/dev/null 2>&1
sleep 8

# --- health ---
ok=0
for i in 1 2 3 4 5 6 7 8 9 10; do
  code="$(curl -s -o /dev/null -w '%{http_code}' "$HEALTH_URL" || true)"
  if [ "$code" = "200" ]; then
    ok=1
    break
  fi
  echo "health: wait ($i) http=$code"
  sleep 3
done

if [ "$ok" != "1" ]; then
  echo "배포 후 헬스체크 실패: $HEALTH_URL"
  echo "로그: tail -100 $TOMCAT_HOME/logs/catalina.out"
  exit 1
fi

echo "DEPLOY_OK  $HEALTH_URL"
echo "commit: $(git rev-parse --short HEAD)"
