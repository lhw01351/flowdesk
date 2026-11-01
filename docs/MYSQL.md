# FlowDesk · MySQL

최종 갱신: 2026-11-01

운영 DB 이름은 데이터 보존을 위해 **`cx_tracker`** 를 유지합니다. (앱 브랜드 FlowDesk와 별개)  
시크릿·환경변수: [ENV.md](ENV.md)

## 구성

| 항목 | 설정 |
|------|--------|
| DB | `cx_tracker` |
| USER | `CX_DB_USER` (예: `cx`) |
| PASS | `CX_DB_PASSWORD` (**필수**, 저장소에 두지 않음) |
| JDBC | `CX_DB_URL` 또는 `jdbc:mysql://127.0.0.1:3306/cx_tracker?...` |
| Spring profile | `mysql` (`setenv.sh` → `SPRING_PROFILES_ACTIVE=mysql`) |

필수 환경변수: `CX_DB_PASSWORD` · 선택: `CX_DB_URL`, `CX_DB_USER` · 시드 계정: `FLOWDESK_PASS_*` (`.env.example` 참고)

## 최초 설치 (WSL)

```bash
bash /mnt/d/ispark_prj/flowdesk/scripts/ubuntu-mysql-setup.sh
```

스크립트가 MySQL 설치·DB/유저 생성·`schema.sql` 적용까지 수행합니다.  
sudo 비밀번호가 필요합니다.

## Tomcat에 mysql 프로필

```bash
cp /mnt/d/ispark_prj/flowdesk/scripts/tomcat-setenv-mysql.sh ~/tomcat/bin/setenv.sh
chmod +x ~/tomcat/bin/setenv.sh
~/tomcat/bin/shutdown.sh
~/tomcat/bin/startup.sh
```

## 테이블

`schema.sql` 기준:

- `inquiries` — 문의
- `inbody_records` — 인바디
- `access_logs` — 접속 로그
- `app_users` — 로그인 회원 (`auth_cd`)
- `fd_auth` — 권한 마스터
- `fd_menu` / `fd_menu_auth` — 메뉴·권한 매핑

앱 기동 시 `SchemaBootstrap`이 `CREATE TABLE IF NOT EXISTS`를 한 번 더 보장합니다.  
인바디 시드(첨부 리포트 기반)는 테이블이 비어 있을 때만 `InbodyBootstrap`이 넣습니다.

## 확인

```bash
mysql -ucx -pcx1234 -e "USE cx_tracker; SHOW TABLES; SELECT COUNT(*) FROM inquiries; SELECT COUNT(*) FROM inbody_records; SELECT COUNT(*) FROM access_logs;"
```
