#!/usr/bin/env bash
# Template committed to Git. Copy to Tomcat and fill secrets locally:
#   cp scripts/tomcat-setenv-mysql.sh ~/tomcat/bin/setenv.sh
#   chmod +x ~/tomcat/bin/setenv.sh
# Real secrets must never be committed. See .env.example

export SPRING_PROFILES_ACTIVE=mysql
export CX_DB_USER="${CX_DB_USER:-cx}"
export CX_DB_PASSWORD="${CX_DB_PASSWORD:?Set CX_DB_PASSWORD in the environment or local setenv.sh}"

export FLOWDESK_DATA_DIR="${FLOWDESK_DATA_DIR:-$HOME/flowdesk-data}"

# 시드 계정 (값이 있을 때만 AuthDataBootstrap 이 신규 생성/동기화)
# 기존 DB에 계정이 있으면 비워 둬도 로그인 가능
export FLOWDESK_PASS_ADMIN="${FLOWDESK_PASS_ADMIN:-}"
export FLOWDESK_PASS_HWLEE="${FLOWDESK_PASS_HWLEE:-}"
export FLOWDESK_PASS_HANI="${FLOWDESK_PASS_HANI:-}"
export FLOWDESK_PASS_GUEST="${FLOWDESK_PASS_GUEST:-guest}"

export CATALINA_OPTS="$CATALINA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -DFLOWDESK_DATA_DIR=${FLOWDESK_DATA_DIR}"
