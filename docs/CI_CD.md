# FlowDesk CI / CD 안내

이 문서는 **확인된 구성(Windows + WSL2 Tomcat)** 기준으로 작성했습니다.

```
[개발자 PC]
   git push
      │
      ▼
[GitHub]
   ├─ CI (클라우드) ……… mvn package 자동 검증
   └─ CD (선택) ………… self-hosted runner → WSL deploy-wsl.sh
                              │
                              ▼
                     [WSL Tomcat /flowdesk]
```

---

## 1. CI (이미 자동화됨)

파일: `.github/workflows/ci.yml`

**언제 도는가**
- `main` / `develop` 에 push
- 위 브랜치로 향하는 Pull Request

**하는 일**
- JDK 17 + Maven으로 `flowdesk.war` 빌드
- 실패하면 Actions에 빨간 X → 머지 전에 알 수 있음

**확인 방법**
1. https://github.com/lhw01351/flowdesk/actions
2. 워크플로 **CI** 클릭 → 최신 실행이 초록이면 성공

로컬에서와 동일 검증:
```bash
cd /mnt/d/ispark_prj/flowdesk
/mnt/d/ispark_prj/tools/apache-maven-3.9.6/bin/mvn -B -DskipTests package
```

---

## 2. 배포 스크립트 (WSL에서 실행)

파일: `scripts/deploy-wsl.sh`

**하는 일 (한 번에)**
1. `git pull` (원하면 생략 가능)
2. Maven으로 WAR 생성
3. Tomcat 중지 → `webapps/flowdesk.war` 교체 → 기동
4. `http://127.0.0.1:8080/flowdesk/login.html` 헬스체크

### 수동 배포 (가장 확실 · 지금 당장 가능)

WSL 터미널:
```bash
cd /mnt/d/ispark_prj/flowdesk
git checkout develop   # 또는 main
git pull
bash scripts/deploy-wsl.sh
```

Windows PowerShell에서 WSL로 실행:
```powershell
wsl -e bash -lc "cd /mnt/d/ispark_prj/flowdesk && bash scripts/deploy-wsl.sh"
```

**환경변수 (선택)**
| 변수 | 의미 | 기본 |
|------|------|------|
| `FLOWDESK_BRANCH` | pull 받을 브랜치 | 현재 브랜치 / develop |
| `FLOWDESK_TOMCAT` | Tomcat 홈 | `$HOME/tomcat` |
| `SKIP_GIT=1` | git pull 생략 | |
| `SKIP_BUILD=1` | 이미 빌드된 WAR만 배포 | |

예:
```bash
SKIP_GIT=1 bash scripts/deploy-wsl.sh
```

**전제**
- `~/tomcat/bin/setenv.sh` 에 mysql 프로필·`CX_DB_PASSWORD`·`FLOWDESK_DATA_DIR` 설정되어 있을 것  
  (저장소에는 비밀번호를 넣지 않음. `scripts/tomcat-setenv-mysql.sh.example` 참고)

---

## 3. CD 자동화 (self-hosted runner)

GitHub 클라우드 서버는 **집 PC의 WSL Tomcat에 직접 들어갈 수 없습니다.**  
(공유기 뒤에 있고, 비밀번호도 Actions에 넣고 SSH 여는 방식은 보안상 비추천)

그래서 자동 배포는 이렇게 합니다:

> **집 PC(또는 WSL)에 GitHub Actions Runner를 설치**  
> → GitHub가 “그 PC에서” `deploy-wsl.sh` 를 실행

파일: `.github/workflows/cd-deploy.yml`
- `main` push 시 (md 제외) 또는 Actions에서 **수동 Run workflow**

### 러너 설치 순서 (당신이 서버/PC에서 할 일)

1. GitHub 저장소 → **Settings** → **Actions** → **Runners** → **New self-hosted runner**
2. OS: **Linux** (WSL Ubuntu) 권장  
   표시되는 명령을 WSL에서 그대로 실행 (토큰은 화면에만 나오고 채팅에 공유 금지)
3. 대략 형태:
   ```bash
   mkdir -p ~/actions-runner && cd ~/actions-runner
   # GitHub 안내의 curl로 tar 다운로드·압축 해제
   ./config.sh --url https://github.com/lhw01351/flowdesk --token <화면에_나온_토큰>
   # 라벨 기본값 사용 (runs-on: self-hosted 와 맞음)
   ./run.sh
   ```
4. 백그라운드 상시 실행(선택):
   ```bash
   sudo ./svc.sh install
   sudo ./svc.sh start
   ```
5. Settings → Runners 에 Idle(대기)로 보이면 준비 완료
6. Actions → **CD Deploy** → **Run workflow** → 브랜치 `develop` 또는 `main`

Windows에 러너를 설치해도 됩니다. 그때는 워크플로가 `wsl.exe`로 `deploy-wsl.sh`를 호출합니다.

### 러너 없이 “반자동”으로 쓰는 법

매일/필요할 때 WSL에서만:
```bash
bash /mnt/d/ispark_prj/flowdesk/scripts/deploy-wsl.sh
```
이것도 포폴에서 “배포 스크립트로 재현 가능한 배포”로 설명 가능합니다.  
완전 무인 CD는 러너 등록 후입니다.

---

## 4. 추천 운영 흐름

```text
기능 작업 → develop push
    → CI 자동 통과 확인
    → (선택) PR develop→main
    → main 머지 후
         A) 러너 있음: CD 자동 또는 수동 Run
         B) 러너 없음: WSL에서 deploy-wsl.sh
```

---

## 5. 문제 생길 때

| 증상 | 확인 |
|------|------|
| CI 실패 | Actions 로그의 Maven 에러 |
| CD가 계속 Queued | Runner가  Offline / 미등록 |
| 배포 후 404·로그인 안 됨 | `tail -f ~/tomcat/logs/catalina.out`, setenv·MySQL |
| 첨부/프로필 깨짐 | `FLOWDESK_DATA_DIR` 절대경로 |
| 포트 외부 접속 안 됨 | `WslPortBridge` + Windows 방화벽 |

---

## 6. 비밀정보

- DB 비밀번호·토큰을 GitHub 코드/채팅에 넣지 마세요.
- Runner 등록 토큰은 **일회성**이며 GitHub 화면에서만 복사합니다.
