# FlowDesk 작업 기록

최종 갱신: 2026-10-26

## 요약

개인 워크스페이스 **FlowDesk** (`/flowdesk`)로 리브랜딩·패키징하고,  
로그인·권한·메뉴·문의·인바디·리마인드·접속 로그까지 한 WAR로 운영합니다.

---

## 2026-10-26 — 문의 개인화 · login-as · 점검

- `inquiries.create_user` 추가, 기존 건 `hwlee` 백필 (`InquiryBootstrap`)
- 문의 API/화면: 본인만 / 01은 대상 사용자 콤보 (`fdMountUserPicker`)
- 회원관리: `POST /api/users/{id}/login` (admin·hwlee) · 접속로그 `LOGIN_AS`
- restricted·관리 API: **admin·hwlee** 화이트리스트로 명확화 (`MenuAccessService`)
- 홈: 날씨·리마인드 높이 동기화(비동기 로드 후 sync)
- 문서 정리 · 구 H2/로그/oracle 스키마 참고본 제거

---

## 2026-10-25 — 홈 2×2 · 리마인드 · Open-Meteo

- 홈 패널: 날씨 · 리마인드 · 예비 영역 · 채팅 (토글)
- 리마인드 CRUD · 메뉴(개인) · 홈 미니캘린더
- 날씨: Open-Meteo + GPS/지역 검색 · CSP `connect-src` 반영

---

## 2026-10-25 — 메뉴 계층 (대 > 중 > 소)

- 구조: 홈 / 업무 페이지(문의·접속로그) / 개인 페이지(인바디) / 관리 페이지(회원·메뉴)
- 대메뉴 클릭 시 펼침·접힘 (상태 localStorage 유지)
- `menuTree` API · 메뉴관리에서 레벨·상위·리프 편집

---

## 2026-10-25 — 인바디 사용자별 소유

- `inbody_records`에 `create_user` / `create_dt` / `modify_dt` 추가
- 일반 사용자: 본인 인바디만 조회·등록·수정·삭제
- 홈 대시보드도 로그인 사용자 기준
- 권한 **01**: 홈·인바디 케어에 **대상 사용자 콤보** — 전원 조회·관리
- 기존 시드 6건 소유자: `hwlee` (시드/보정 기본값도 `hwlee`)

---

## 2026-10-25 — 권한·메뉴·계정 정리

### 계정

| 아이디 | 이름 | 권한 | 비고 |
|--------|------|------|------|
| `admin` | 관리자 | 01 | 구 `cxadmin` 개명. 비번 `cxadmin123` |
| `hwlee` | 이현욱 | 01 | 회원·메뉴 관리 가능 |
| `hani` | 김하니 | 01 | 비번 `hani1234`. 업무/개인 메뉴 (회원·메뉴 관리 제외) |
| `guest` | 손님 | 02 | 비번 `guest1234`. 홈·인바디·리마인드 |

### 권한 테이블 `fd_auth`

| 코드 | 명칭 | 비고 |
|------|------|------|
| `01` | 전체관리자 | `create_dt` 포함 |
| `02` | 일반사용자 | |

회원 화면에서 `auth_cd` 부여 가능.

### 메뉴 테이블 `fd_menu` / `fd_menu_auth`

ispark `com_pgm` · `com_pgm_auth` 참고.  
필수: `menu_id`, `menu_key`, `menu_nm`, `page_url`, `api_prefix`, 정렬, `use_yn`, `restricted_yn`  
확장: `up_menu_id`, `path`, `new_url`, `link_url`, `icon`, `guide_comment`, `create_dt`/`update_dt`/`delete_dt` 등

시드 메뉴: 홈 · 문의 트래커 · 인바디 케어 · 접속 로그 · 회원 관리 · 메뉴 관리  
`restricted_yn=Y`(회원·메뉴 관리) → **admin · hwlee** 만 (`MenuAccessService.SUPER_ADMINS`)

### 화면

- `users.html` — 회원 등록/수정 + 권한 부여 (admin·hwlee)
- `menus.html` — 메뉴명·허용 권한·정렬·사용여부 (admin·hwlee)
- 좌측 셸은 `/api/auth/me`의 `menuList`로 동적 렌더

### 정리한 찌꺼기

- 구 portproxy 스크립트 (WslPortBridge로 대체됨)
- 구 Windows 서비스 스크립트 (`CxInquiryTracker` 잔여)
- `build.bat` / `start.bat` 구 artifact명(`cx-inquiry-tracker`) 수정
- 미사용 `AuthController.isAdmin/isStaff/isWork` 제거
- 관리자 API(`/api/auths` 등)는 메뉴 매핑과 무관하게 admin·hwlee 통과하도록 `AuthFilter` 보정

### 문서 · 테스트

- `docs/CHANGELOG.md` 신설, `OVERVIEW`/`README`/`DEPLOY` 갱신
- `scripts/smoke-test.sh` — 정적·로그인·권한·헤더 스모크

---

## 이전 작업 (같은 일자 · 누적)

1. **리브랜딩** — `cx-inquiry-tracker` → FlowDesk (`kr.flowdesk`, WAR `/flowdesk`)
2. **보안** — CSP 등 헤더, `HtmlEscape`, 세션 쿠키, `AuthFilter`
3. **인바디 케어** — 측정 CRUD, 홈 대시보드, 기간 필터
4. **접속 로그** — 로그인/페이지 · 기기 · GPS
5. **ROOT `/` · `/test` 리다이렉트** — Tomcat ROOT 정적 파일
6. **WSL 포트** — `WslPortBridge` (Windows 8080 → WSL Tomcat)
7. **문서** — `README.md`, `docs/OVERVIEW.md`, `DEPLOY.md`, `MYSQL.md`

---

## 테스트 체크 (수동/스크립트)

`scripts/smoke-test.sh` (WSL, Tomcat 기동 후):

- 로그인 4계정 + 구 계정 거부
- 권한별 메뉴·API 200/403
- 정적 CSS/JS · login.html 200
