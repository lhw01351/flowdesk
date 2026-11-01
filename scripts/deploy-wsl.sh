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

step() {
  echo ""
  echo "============================================================"
  echo "  $1"
  echo "============================================================"
}

ok_line() {
  echo "  [OK] $1"
}

info() {
  echo "  - $1"
}

warn() {
  echo "  [!] $1"
}

fail() {
  echo ""
  echo "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
  echo "  실패: $1"
  echo "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
  exit 1
}

cd "$REPO_DIR"

echo ""
echo "############################################################"
echo "#  FlowDesk 배포를 시작합니다"
echo "############################################################"
info "프로젝트 폴더: $REPO_DIR"
info "Tomcat 경로:   $TOMCAT_HOME"
info "시작 시각:     $(date '+%Y-%m-%d %H:%M:%S')"

step "[1/6] 사전 점검 (Tomcat · Maven)"
if [ ! -x "$TOMCAT_HOME/bin/catalina.sh" ]; then
  fail "Tomcat을 찾을 수 없습니다 → $TOMCAT_HOME"
fi
ok_line "Tomcat 실행 스크립트 확인"

if [ ! -x "$MVN_BIN" ]; then
  if command -v mvn >/dev/null 2>&1; then
    MVN_BIN="$(command -v mvn)"
  else
    fail "Maven을 찾을 수 없습니다. FLOWDESK_MVN 또는 tools/apache-maven 경로를 확인하세요."
  fi
fi
ok_line "Maven 확인 → $MVN_BIN"

step "[2/6] GitHub에서 최신 코드 받기"
if [ "${SKIP_GIT:-0}" != "1" ]; then
  if [ -z "$BRANCH" ]; then
    BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || true)"
  fi
  if [ -z "$BRANCH" ] || [ "$BRANCH" = "HEAD" ]; then
    BRANCH="develop"
  fi
  info "대상 브랜치: $BRANCH"
  info "git fetch 중…"
  git fetch origin
  info "브랜치 전환 중…"
  git checkout "$BRANCH"
  info "git pull 중…"
  git pull --ff-only origin "$BRANCH"
  ok_line "코드 동기화 완료 ($(git rev-parse --short HEAD))"
else
  warn "SKIP_GIT=1 → Git 동기화를 건너뜁니다"
  ok_line "현재 폴더 코드로 진행"
fi

step "[3/6] 프로젝트 빌드 (WAR 만들기)"
if [ "${SKIP_BUILD:-0}" != "1" ]; then
  info "Maven package 실행 중… (1~2분 걸릴 수 있습니다)"
  "$MVN_BIN" -B -DskipTests clean package
  ok_line "빌드 완료"
else
  warn "SKIP_BUILD=1 → 빌드를 건너뜁니다"
fi

WAR="$REPO_DIR/target/flowdesk.war"
if [ ! -f "$WAR" ]; then
  fail "WAR 파일이 없습니다 → $WAR"
fi
ok_line "배포 파일 확인 → target/flowdesk.war"

step "[4/6] Tomcat 중지"
info "Tomcat 종료 요청 중…"
"$TOMCAT_HOME/bin/shutdown.sh" >/dev/null 2>&1 || true
sleep 3
if pgrep -f "catalina.base=$TOMCAT_HOME" >/dev/null 2>&1 || pgrep -f "org.apache.catalina.startup.Bootstrap" >/dev/null 2>&1; then
  warn "잔여 Java 프로세스 발견 → 강제 종료"
  pkill -f "org.apache.catalina.startup.Bootstrap" >/dev/null 2>&1 || true
  sleep 2
fi
ok_line "Tomcat 중지 완료"

step "[5/6] WAR 배포 후 Tomcat 기동"
info "기존 flowdesk 앱 제거 중…"
rm -rf "$TOMCAT_HOME/webapps/flowdesk" "$TOMCAT_HOME/webapps/flowdesk.war"
info "새 WAR 복사 중…"
cp "$WAR" "$TOMCAT_HOME/webapps/flowdesk.war"
ok_line "WAR 복사 완료"

if [ ! -f "$TOMCAT_HOME/bin/setenv.sh" ]; then
  warn "setenv.sh 없음 → MySQL 설정/업로드 경로가 빠질 수 있습니다"
  info "예: cp scripts/tomcat-setenv-mysql.sh $TOMCAT_HOME/bin/setenv.sh"
else
  ok_line "setenv.sh 확인"
fi

info "Tomcat 시작 중…"
nohup "$TOMCAT_HOME/bin/catalina.sh" start >/dev/null 2>&1
sleep 8
ok_line "Tomcat 기동 요청 완료"

step "[6/6] 접속 확인 (헬스체크)"
info "확인 주소: $HEALTH_URL"
ok=0
for i in 1 2 3 4 5 6 7 8 9 10; do
  code="$(curl -s -o /dev/null -w '%{http_code}' "$HEALTH_URL" || true)"
  if [ "$code" = "200" ]; then
    ok=1
    ok_line "응답 HTTP $code (시도 $i/10)"
    break
  fi
  info "대기 중… 시도 $i/10 (HTTP $code)"
  sleep 3
done

if [ "$ok" != "1" ]; then
  fail "헬스체크 실패 → $HEALTH_URL  (로그: tail -100 $TOMCAT_HOME/logs/catalina.out)"
fi

echo ""
echo "############################################################"
echo "#  배포 성공!"
echo "############################################################"
ok_line "로그인 페이지: $HEALTH_URL"
ok_line "현재 커밋:     $(git rev-parse --short HEAD)"
ok_line "종료 시각:     $(date '+%Y-%m-%d %H:%M:%S')"
echo ""
info "브라우저에서 위 주소로 확인하세요."
info "게스트 버튼: guest / guest"
echo ""
