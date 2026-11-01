# FlowDesk CI / CD 안내

이 문서는 **확인된 구성(Windows + WSL2 Tomcat)** 과 **공개(Public) GitHub 저장소** 기준입니다.

---

## 보안 (중요) — Self-hosted Runner

**공개 저장소에 Self-hosted Runner를 연결하지 마세요.**

외부 기여자가 PR로 워크플로를 건드리면, 그 코드가 **당신 PC(러너)에서 실행**될 수 있습니다.  
GitHub도 public repo + self-hosted runner 조합을 강하게 경고합니다.

| 구성 | 공개 repo에서 |
|------|----------------|
| **CI = GitHub-hosted runner** (`ubuntu-latest`) | ✅ 권장 (현재 적용) |
| **CD = WSL에서 `deploy-wsl.sh` 수동/로컬 실행** | ✅ 권장 |
| **CD = Self-hosted runner** | ❌ 공개 repo에선 비권장 |
| Self-hosted | Private repo + 제한된 워크플로에서만 검토 |

비밀번호는 Git 코드에 넣지 말고 **서버 `setenv.sh` / 환경변수**로만 관리합니다.

---

## 추천 구성 (지금 FlowDesk)

```
개발자: git push
        │
        ▼
GitHub (Public) ── CI: GitHub-hosted ─── mvn package 검증만
        │
        │  (사람 승인 / 본인 PC에서)
        ▼
WSL: bash scripts/deploy-wsl.sh ──► Tomcat /flowdesk
```

- **CI:** 자동 (클라우드 빌려서 빌드)  
- **CD:** 본인이 신뢰하는 PC에서 배포 스크립트 실행 (집 Tomcat)

---

## 1. CI (이미 동작 중)

파일: `.github/workflows/ci.yml`  
`runs-on: ubuntu-latest` → **GitHub가 빌려 주는 PC** (self-hosted 아님)

**언제:** `main` / `develop` push, 또는 그 브랜치로 PR  
**하는 일:** JDK 17 + Maven으로 WAR 빌드 검증  

확인: https://github.com/lhw01351/flowdesk/actions

---

## 2. CD / 배포 (WSL 스크립트)

파일: `scripts/deploy-wsl.sh`

Self-hosted runner 없이, **당신 PC의 WSL**에서:

```bash
cd /mnt/d/ispark_prj/flowdesk
git pull
bash scripts/deploy-wsl.sh
```

PowerShell:
```powershell
wsl -e bash -lc "cd /mnt/d/ispark_prj/flowdesk && bash scripts/deploy-wsl.sh"
```

**하는 일:** git pull → `mvn package` → Tomcat 재시작 → 로그인 페이지 헬스체크

전제: `~/tomcat/bin/setenv.sh`에 DB·`FLOWDESK_DATA_DIR` (비밀번호는 로컬만)

---

## 3. “Runner부터”에 대한 정리

헷갈리기 쉬운 점:

| 말 | 실제 |
|----|------|
| CI runner | **GitHub-hosted** (`ubuntu-latest`) ← 이미 설정됨, 설치 불필요 |
| Self-hosted runner | 집 PC에 설치하는 러너 ← **공개 repo면 하지 말 것** |

그래서 다음 작업은 **러너 설치가 아니라**:
1. CI Actions가 초록인지 확인
2. WSL에서 `deploy-wsl.sh`로 배포 연습
3. 포폴 문서/화면 캡처 등

나중에 저장소를 **Private**로 바꾸거나, 배포 전용 private 저장소를 두면 그때 self-hosted CD를 다시 검토하면 됩니다.

---

## 4. 일상 흐름

```text
기능 수정 → develop push
  → Actions CI 자동 통과 확인
  → (원할 때) WSL에서 deploy-wsl.sh
  → 브라우저로 /flowdesk 확인
  → 안정되면 PR develop → main
```
