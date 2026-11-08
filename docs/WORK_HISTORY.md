# FlowDesk 작업 이력 (포폴·인수인계)

최종 갱신: 2026-11-08  
범위: 공개 GitHub 저장소 정리 · UI · 보안 · CI/CD · WSL Tomcat 운영까지 누적 작업

---

## 1. 제품 한 줄

Spring Boot WAR 기반 **개인/소규모 워크스페이스**.  
로그인·권한·메뉴 · 문의 트래커 · 인바디 · 리마인드 · 홈(날씨/채팅) · 접속 로그를 `/flowdesk` 한 컨텍스트로 배포.

---

## 2. 아키텍처

```
브라우저 → Windows:8080 (WslPortBridge) → WSL Tomcat:8080/flowdesk → MySQL(cx_tracker)
                                              ↘ FLOWDESK_DATA_DIR (업로드 절대경로)
```

| 구분 | 내용 |
|------|------|
| Backend | Java 11+/17, Spring Boot 2.7, MyBatis, 세션 인증 |
| Frontend | HTML/CSS/JS, Chart.js, 앱 셸(사이드바) |
| DB | MySQL 8 (`SPRING_PROFILES_ACTIVE=mysql`) / 로컬 H2 |
| Deploy | Maven WAR → Tomcat 9 (WSL) · `scripts/deploy-wsl.sh` |
| CI | GitHub Actions `ci.yml` (빌드만, hosted runner) |

---

## 3. 패키지 구조 (정리 후)

```
kr.flowdesk
├── config/          필터·부트스트랩·WebSocket·시드/레이트미터
├── domain / mapper
├── service/
│   ├── system/      AppUser, FdAuth, FdMenu, MenuAccess, AccessLog
│   └── content/     Inquiry, Inbody, Reminder, Chat, Sla
├── web/
│   ├── system/      Auth, AppUser, FdAuth, FdMenu, AccessLog, AppError
│   └── content/     Inquiry, Inbody, Reminder, Chat
└── util/            HtmlEscape
```

---

## 4. 주요 기능 작업

| 영역 | 내용 |
|------|------|
| 인증·권한 | 세션 로그인, `fd_auth` 01/02, 메뉴 매핑, restricted(admin·hwlee) |
| 게스트 | 포폴용 `guest`/`guest` · 본인 데이터만 |
| 홈 | 날씨(Open-Meteo) · 리마인드 · 채팅 · 접이식 패널 |
| 문의 | 소유자 분리 · SLA · 관리자 콤보 |
| 인바디·리마인드 | 사용자별 CRUD |
| 회원/메뉴 | CRUD · login-as · 접속 로그 |
| UI 셸 | 사이드바 너비/접기 · 헤더 압축 · 사이드바 로그아웃 · 세션 연장(↻) |
| 세션 | 유휴 **2시간** (`application.yml` + `web.xml` + ServletContext) |

---

## 5. 보안·시크릿 정리

| 작업 | 설명 |
|------|------|
| YAML 계정 제거 | 로그인 = MySQL `app_users`만 |
| env 시크릿 | `CX_DB_PASSWORD`, `FLOWDESK_PASS_*` · `.env.example` |
| Git 히스토리 | 평문 비번 제거를 위해 공개 스냅샷으로 재작성(force push) |
| 문서 슬림화 | 개인 PC 인수인계 MD·중복 스크립트 삭제 |
| 실패 로그인 | `LOGIN_FAIL` 접속로그 + IP 레이트 리밋(15분/8회) |
| 스캐너 404 | 미로그인 시 알 수 없는 경로 → 로그인 리다이렉트 대신 **404** |
| 방화벽 스크립트 | 8080 기본 **Private**만 허용 · 닫기 스크립트 추가 |

상세 점검 결과: [SECURITY_AUDIT.md](SECURITY_AUDIT.md)

---

## 6. CI/CD · 운영

- **CI:** push/PR → Maven package (GitHub-hosted)
- **CD:** 공개 repo에 self-hosted runner **비사용** · 로컬 `deploy-wsl.sh`
- 업로드 깨짐 방지: `FLOWDESK_DATA_DIR` 절대경로
- 시작/중지/재기동: [OPS_SERVICE.md](OPS_SERVICE.md)

---

## 7. 관련 문서

| 문서 | 용도 |
|------|------|
| [README](../README.md) | 진입·요약 |
| [PORTFOLIO](PORTFOLIO.md) | 채용 한 장 |
| [OVERVIEW](OVERVIEW.md) | 모듈·API |
| [DEPLOY](DEPLOY.md) | 배포·브릿지 |
| [ENV](ENV.md) | 환경변수 |
| [CI_CD](CI_CD.md) | CI/CD 정책 |
| [OPS_SERVICE](OPS_SERVICE.md) | 서비스 기동 |
| [SECURITY_AUDIT](SECURITY_AUDIT.md) | 접근 점검·권고 |
