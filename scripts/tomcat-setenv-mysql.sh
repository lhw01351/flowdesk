#!/usr/bin/env bash
# Template committed to Git. For real passwords use a local copy:
#   cp scripts/tomcat-setenv-mysql.sh.example ~/tomcat/bin/setenv.sh
# Real secrets must never be committed.

export SPRING_PROFILES_ACTIVE=mysql
export CX_DB_USER="${CX_DB_USER:-cx}"
export CX_DB_PASSWORD="${CX_DB_PASSWORD:?Set CX_DB_PASSWORD in the environment or local setenv.sh}"

export FLOWDESK_DATA_DIR="${FLOWDESK_DATA_DIR:-$HOME/flowdesk-data}"

export CATALINA_OPTS="$CATALINA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -DFLOWDESK_DATA_DIR=${FLOWDESK_DATA_DIR}"
