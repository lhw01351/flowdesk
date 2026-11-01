#!/usr/bin/env bash
set -e
DB_USER="${CX_DB_USER:-cx}"
DB_PASS="${CX_DB_PASSWORD:-}"
if [ -z "$DB_PASS" ]; then
  echo "CX_DB_PASSWORD 환경변수가 필요합니다."
  exit 1
fi

echo "=== table / count ==="
mysql -u"$DB_USER" -p"$DB_PASS" -e "USE cx_tracker; SHOW TABLES; SELECT COUNT(*) AS cnt FROM inquiries;"

echo "=== API GET ==="
curl -s -w "\nhttp=%{http_code}\n" http://127.0.0.1:8080/flowdesk/api/inquiries | head -c 500
echo

echo "=== POST test ==="
curl -s -w "\nhttp=%{http_code}\n" -X POST http://127.0.0.1:8080/flowdesk/api/inquiries \
  -H 'Content-Type: application/json; charset=utf-8' \
  -d '{"direction":"인바운드","channel":"온라인","status":"완료","category":"일반 문의","module":"인사평가","inboundAt":"2026-10-20T15:00","firstResponseAt":"2026-10-20T15:10","resolvedAt":"2026-10-20T15:20","customer":"MySQL테스트","reviewer":"테스터","note":"api검증","linkedId":""}'
echo

echo "=== rows ==="
mysql -u"$DB_USER" -p"$DB_PASS" -e "SELECT id,customer,category FROM cx_tracker.inquiries ORDER BY id DESC LIMIT 5;"

echo "=== recent errors ==="
grep -E 'ERROR|Exception|Communications|Access denied' ~/tomcat/logs/catalina.out | tail -15 || true
