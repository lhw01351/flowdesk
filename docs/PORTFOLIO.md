# FlowDesk — 포트폴리오

> **한 줄**  
> 화면 CRUD가 아니라, **권한·소유권·배포·운영**까지 포함한 실사용 웹앱을 FlowDesk로 설계·구현했습니다.

---

## 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 이름 | **FlowDesk** · My Workspace |
| 기간 | 2026.10 (개인 프로젝트) |
| 역할 | 기획 · 풀스택 구현 · WSL Tomcat 배포 · 외부 접속 검증 |
| 형태 | Spring Boot WAR 모놀리스 (`/flowdesk`) |
| URL | `http://<호스트>:8080/flowdesk/login.html` |

업무 문의 트래킹과 개인 루틴(인바디·리마인드·날씨)을 **한 계정·한 메뉴 체계**로 묶고,  
Windows + WSL2 Tomcat + MySQL 환경에서 실제 접속 가능한 서비스로 올렸습니다.

---

## 기술 스택

- **Backend:** Java 17 · Spring Boot 2.7 · MyBatis · 세션 인증
- **Frontend:** HTML/CSS/JS · Chart.js · 반응형 셸(모바일 드로어)
- **DB:** MySQL 8 (`cx_tracker`) / 로컬 H2
- **Deploy:** Maven WAR → Apache Tomcat 9 (WSL2) · Windows 포트 브릿지

---

## 핵심 기능

| 영역 | 내용 |
|------|------|
| 인증·권한 | 세션 로그인, `fd_auth`(01/02), 메뉴·API 접근 제어 |
| 메뉴 | 대·중·소 트리, 권한 다중 매핑, restricted(회원·메뉴)는 admin·hwlee |
| 홈 | 뉴스/주식 라이브 · 리마인드 캘린더 · Open-Meteo 날씨 · 인바디 요약 |
| 리마인드 | 사용자별 일정 CRUD · 캘린더 |
| 문의 트래커 | CRUD · SLA · 차트 · **사용자별 소유** (01은 대상 사용자 콤보) |
| 인바디 케어 | 체성분 입력·추이 · **create_user 소유** (01은 교차 조회) |
| 회원 관리 | 계정·권한 · **해당 사용자로 로그인(login-as)** |
| 접속 로그 | 기기 · GPS · 시각 · LOGIN_AS 감사 |

---

## 내가 한 일 (강조)

1. **권한은 화면 숨김이 아니라 API까지**  
   `AuthFilter` + `MenuAccessService` — 메뉴 prefix·restricted·관리 API를 서버에서 차단

2. **도메인별 데이터 소유권**  
   - 문의·인바디: `create_user` + `?user=` + 관리자 콤보  
   - 리마인드: 세션 사용자 전용  
   - 기존 문의 데이터는 `hwlee`로 백필

3. **운영형 보안 헤더**  
   CSP에 YouTube·korulive·Open-Meteo `frame-src`/`connect-src` 반영 (iframe·날씨 API 실사용)

4. **WSL2 실배포**  
   Tomcat WAR + Windows `WslPortBridge`·방화벽으로 LAN/LTE 외부 접속 검증

5. **관리 편의**  
   회원 목록에서 비밀번호 없이 대상 계정 세션 전환(`LOGIN_AS` 로그)

---

## 아키텍처

```
Browser → Windows:8080 → WslPortBridge → Tomcat(/flowdesk) → MySQL(cx_tracker)
                              ↓
              AuthFilter / MenuAccess / SecurityHeaders
```

로그인 → `app_users.auth_cd` → `fd_menu` + `fd_menu_auth` → 사이드 메뉴·API.

---

## 면접에서 말할 포인트

| 질문 예 | 답변 축 |
|---------|---------|
| 왜 모놀리식 WAR인가? | 소규모 실사용에 배포·백업·롤백이 단순. 모듈은 패키지(`web/service/mapper`)로 분리 |
| 권한을 어떻게 막았나? | 프론트 메뉴 숨김 + 필터에서 path/API prefix 검사 + restricted는 계정 화이트리스트 |
| 멀티테넌시? | 테이블에 소유 컬럼, 목록·CRUD·import/export 모두 owner 스코프 |
| 트러블슈팅 | CSP로 막힌 YouTube iframe, WSL 포트 미노출, 그리드 stretch로 닫힌 카드가 늘어나던 UX |

---

## 데모 계정

| 계정 | 비밀번호 | 역할 |
|------|----------|------|
| `hwlee` | `hwlee1234` | 전체관리 + 회원/메뉴 관리 |
| `admin` | `cxadmin123` | 동일 (관리) |
| `hani` | `hani1234` | 권한 01 · 업무/개인 메뉴 (회원·메뉴 관리 제외) |
| `guest` | `guest1234` | 권한 02 · 홈·인바디·리마인드 |

---

## 관련 문서

- [README](../README.md) — 실행·배포 요약  
- [OVERVIEW](OVERVIEW.md) — 모듈·API·테이블  
- [DEPLOY](DEPLOY.md) — Tomcat · 외부 접속  
- [CHANGELOG](CHANGELOG.md) — 작업 이력  
