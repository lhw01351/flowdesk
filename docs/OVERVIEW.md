# FlowDesk 개요

최종 갱신: 2026-10-26

상세 작업 이력은 [CHANGELOG.md](CHANGELOG.md)를 참고하세요.

## 브랜드 · 패키징

| 구분 | 값 |
|------|------|
| 제품명 | **FlowDesk** |
| 카피 | My Workspace · 업무와 루틴을 한곳에서 |
| groupId / artifactId | `kr.flowdesk` / `flowdesk` |
| WAR / context | `flowdesk.war` → **`/flowdesk`** |
| 루트 패키지 | `kr.flowdesk` |
| 애플리케이션 | `kr.flowdesk.FlowDeskApplication` |
| 프로젝트 경로 | `D:\ispark_prj\flowdesk` |

## 모듈 구조

```
kr.flowdesk
├── FlowDeskApplication
├── config/     AuthFilter, AuthDataBootstrap, SchemaBootstrap,
│               InbodyBootstrap, InquiryBootstrap, SecurityHeadersFilter, WebConfig
├── domain/     Inquiry, InbodyRecord, Reminder, AccessLog, AppUser, FdAuth, FdMenu, SlaResult
├── mapper/     MyBatis
├── service/    Inquiry / Inbody / Reminder / AccessLog / AppUser /
│               FdAuth / FdMenu / MenuAccess / Sla
├── web/        Auth, Inquiry, Inbody, Reminder, AccessLog,
│               AppUser, FdAuth, FdMenu Controllers
└── util/       HtmlEscape
```

```
src/main/resources/static/
├── css/theme.css, app-shell.css
├── js/app-shell.js          # 셸·인증·menuTree·사용자 콤보·XSS escape
├── login.html / home.html
├── index.html               # 문의 트래커 (소유자 콤보)
├── health.html / remind.html / access.html
├── users.html               # 회원·login-as (admin·hwlee)
├── menus.html               # 메뉴·권한 매핑 (admin·hwlee)
└── access.html              # 로그인 로그 (admin·hwlee)
```

## 화면 · 권한 흐름

```
login.html
  → 세션(username, auth_cd, role)
  → /api/auth/me → menus + menuList + menuTree
  → app-shell 좌측 메뉴 동적 표시

auth_cd 01
  → 매핑된 업무/개인 메뉴
  → 문의·인바디: 대상 사용자 콤보로 타인 데이터 조회 가능
  → restricted(회원·메뉴): admin·hwlee 만

auth_cd 02
  → 시드: 홈 · 인바디 · 리마인드
  → 본인 데이터만
```

## API 요약

| 경로 | 용도 | 접근 |
|------|------|------|
| `POST /api/auth/login` | 로그인 (+ GPS 선택) | 공개 |
| `POST /api/auth/logout` | 로그아웃 | 로그인 |
| `GET  /api/auth/me` | 세션·메뉴 | 공개(미로그인 401) |
| `/api/inquiries/**` | 문의 (owner `?user=` / create_user) | 메뉴 권한 |
| `/api/inbody/**` | 인바디 (owner) | 메뉴 권한 |
| `/api/reminders/**` | 리마인드 (세션 사용자) | 메뉴 권한 |
| `/api/access-logs` POST | 페이지 접속 기록 | 로그인 사용자 |
| `/api/access-logs` GET | 로그인 로그 조회 | admin·hwlee |
| `/api/users/**` | 회원 CRUD · `POST /{id}/login` | admin·hwlee |
| `/api/auths/**` | 권한 마스터 | admin·hwlee |
| `/api/menus/**` | 메뉴·권한 매핑 | admin·hwlee |

## 계정 · 권한

| 계정 | 이름 | 권한 | 비고 |
|------|------|------|------|
| `admin` | 관리자 | 01 | 회원·메뉴 관리 |
| `hwlee` | 이현욱 | 01 | 회원·메뉴 관리 |
| `hani` | 김하니 | 01 | 업무/개인 (restricted 제외) |
| `guest` | 손님 | 02 | 홈·인바디·리마인드 |

시드·개명은 `AuthDataBootstrap`이 담당합니다.

## 데이터

| 테이블 | 내용 |
|--------|------|
| `inquiries` | 문의 (`create_user` 개인 소유) |
| `inbody_records` | 인바디 (`create_user`) |
| `reminders` | 리마인드 (`username`) |
| `access_logs` | 접속 로그 (LOGIN / LOGIN_AS 등) |
| `app_users` | 로그인 회원 (`auth_cd`) |
| `fd_auth` | 권한 마스터 (01/02) |
| `fd_menu` / `fd_menu_auth` | 메뉴 · 권한 매핑 |

운영 DB 이름 `cx_tracker` 는 데이터 보존을 위해 유지합니다.

## 보안 기본선

| 항목 | 구현 |
|------|------|
| 보안 헤더 | `SecurityHeadersFilter` (CSP: Open-Meteo·WebSocket·blob 등) |
| XSS (서버) | `HtmlEscape.sanitizeText` |
| XSS (클라이언트) | `cxEscapeHtml` |
| 세션 쿠키 | `http-only`, `same-site: lax` |
| 접근 제어 | `AuthFilter` + `MenuAccessService` |

## 스타일 규칙

- 색·타이포·카드·버튼·기간 툴바: `theme.css` 의 `fd-*`
- 사이드바: `app-shell.css`
- 브랜드 문구 변경 시 `app-shell.js` + `login.html` 함께 수정
