# FlowDesk

**My Workspace · 업무와 루틴을 한곳에서**

Spring Boot 기반의 개인·소규모 팀용 워크스페이스입니다.  
로그인·권한·메뉴 트리·문의 트래커·인바디 기록·접속 로그를 **하나의 WAR**로 묶어 Tomcat/MySQL에 배포합니다.

> 포트폴리오 한 줄  
> *“화면 CRUD가 아니라, 권한·배포·운영까지 포함한 실사용 웹앱을 설계·구현했습니다.”*

---

## 왜 만들었나

- 업무 문의 트래킹과 개인 루틴(인바디)을 한 계정·한 메뉴 체계로 쓰고 싶었습니다.
- 권한(01/02)·메뉴 매핑·소유자 기반 데이터 분리를 처음부터 전제로 잡았습니다.
- Windows + WSL2 Tomcat 환경에서 **실제 접속 가능한 서비스**로 올려 검증했습니다.

## 핵심 기능

| 영역 | 내용 |
|------|------|
| 인증·권한 | 세션 로그인, `fd_auth`(01 관리 / 02 일반), 메뉴별 접근 제어 |
| 메뉴 | 대·중·소 트리, 권한 다중 매핑, 관리 화면 |
| 홈 | 리마인드 캘린더 · Open-Meteo 날씨(GPS/검색) · 인바디 요약 · 실시간 채팅 |
| 리마인드 | 사용자별 일정 CRUD · 캘린더 (개인 페이지) |
| 문의 트래커 | CRUD · SLA · 차트 · 사용자별 소유 (01 콤보) |
| 인바디 케어 | 체성분 입력·기간 조회·추이 (소유자 필터, 01은 타인 조회) |
| 회원 관리 | 계정·권한 · 해당 사용자로 로그인 (admin·hwlee) |
| 접속 로그 | 기기·GPS·시각 · LOGIN_AS |
| 운영 | MySQL, CSP 등 보안 헤더, LAN/외부 접속 브릿지 |

## 기술 스택

- **Backend:** Java 11+/17, Spring Boot 2.7, MyBatis, 세션 인증
- **Frontend:** HTML/CSS/JS, Chart.js, 반응형 셸(모바일 드로어 내비)
- **DB:** MySQL 8 (`cx_tracker`) / 로컬 H2
- **Deploy:** Maven WAR → Apache Tomcat 9 (WSL2)
- **CI/CD:** GitHub Actions CI + WSL `scripts/deploy-wsl.sh` (CD는 self-hosted runner 선택)

자세한 배포·Actions 안내: [docs/CI_CD.md](docs/CI_CD.md)

## 아키텍처 (요약)

```
Browser → Windows:8080 → WslPortBridge → Tomcat(/flowdesk) → MySQL(cx_tracker)
                ↘ AuthFilter / MenuAccess / SecurityHeaders
```

권한 흐름: 로그인 → `app_users.auth_cd` → `fd_menu` + `fd_menu_auth` → 사이드 메뉴·API 접근.

## 문제 해결 사례 (포폴 포인트)

1. **데이터 소유권** — 문의·인바디 `create_user` · 01 대상 사용자 콤보 (guest는 본인만)  
2. **WSL2 외부 접속** — Windows 포트 브릿지 + 방화벽으로 LAN 검증  
3. **메뉴 계층** — DB 트리 + 접이식 내비 · restricted는 admin·hwlee  
4. **업로드 경로** — 상대경로 cwd 이탈로 첨부 깨짐 → `FLOWDESK_DATA_DIR` 절대경로 고정

## 화면

| 메뉴 | 파일 | 설명 |
|------|------|------|
| 로그인 | `login.html` | 세션 · 접속 로그(기기/GPS) |
| 홈 | `home.html` | 리마인드 · 날씨 · 인바디 요약 · 채팅 |
| 문의 트래커 | `index.html` | CX CRUD · SLA · 소유자 콤보 |
| 인바디 케어 | `health.html` | 체성분 입력 · 추이 |
| 리마인드 | `remind.html` | 일정 CRUD · 캘린더 |
| 로그인 로그 | `access.html` | 로그인·메뉴·기기·GPS (admin·hwlee) |
| 회원 관리 | `users.html` | 계정·권한 · login-as (admin·hwlee) |
| 메뉴 관리 | `menus.html` | 메뉴·권한 매핑 (admin·hwlee) |

## 로컬 실행 (H2)

```bash
cd D:\ispark_prj\flowdesk
mvn -DskipTests spring-boot:run
```

- URL: http://localhost:8080/
- 계정: `admin`/`cxadmin123`, `hwlee`/`hwlee1234`, `hani`/`hani1234`, `guest`/`guest1234`
- 회원·메뉴 관리: `admin`, `hwlee` (권한 01)

## WAR 배포 (WSL Tomcat + MySQL)

```bash
mvn -DskipTests package
cp /mnt/d/ispark_prj/flowdesk/target/flowdesk.war ~/tomcat/webapps/
~/tomcat/bin/shutdown.sh; ~/tomcat/bin/startup.sh
```

접속: `http://<호스트>:8080/flowdesk/login.html`  
자세한 절차: [docs/DEPLOY.md](docs/DEPLOY.md)

## 문서

- [포트폴리오 한 장](docs/PORTFOLIO.md)
- [개요 · 구조 · 규칙](docs/OVERVIEW.md)
- [작업 이력](docs/CHANGELOG.md)
- [빌드 · Tomcat · 외부 접속](docs/DEPLOY.md)
- [MySQL 설정](docs/MYSQL.md)

## 보안

- CSP, `X-Frame-Options`, `X-Content-Type-Options` (`SecurityHeadersFilter`)
- 저장 시 sanitize / 화면 `cxEscapeHtml`
- 세션 쿠키 `HttpOnly`, `SameSite=Lax`

## 프로젝트 메타

| 항목 | 값 |
|------|-----|
| Maven | `kr.flowdesk:flowdesk` |
| 패키지 | `kr.flowdesk.*` |
| 진입점 | `FlowDeskApplication` |
| Context | `/flowdesk` |
