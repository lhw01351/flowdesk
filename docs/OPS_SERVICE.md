# FlowDesk 서비스 시작 · 중지 · 재기동

최종 갱신: 2026-11-08  
환경: **Windows + WSL2 Ubuntu + Tomcat 9 + MySQL**

---

## 한눈에 보기

| 할 일 | 명령 (PowerShell 또는 WSL) |
|------|---------------------------|
| **전체 재배포(권장)** | `wsl -e bash /mnt/d/ispark_prj/flowdesk/scripts/deploy-wsl.sh` |
| Tomcat만 시작 | WSL: `~/tomcat/bin/startup.sh` |
| Tomcat만 중지 | WSL: `~/tomcat/bin/shutdown.sh` |
| 포트 브릿지 | Windows에서 `WslPortBridge.exe` (또는 시작프로그램) |
| 로그인 URL | `http://127.0.0.1:8080/flowdesk/login.html` |

배포 스크립트가 **중지 → WAR 교체 → 기동 → 헬스체크**까지 합니다.

---

## 1. 사전 조건

1. WSL2 Ubuntu, Tomcat (`~/tomcat` 기본)
2. MySQL 기동 · DB `cx_tracker`
3. `~/tomcat/bin/setenv.sh` 존재  
   - `SPRING_PROFILES_ACTIVE=mysql`  
   - `CX_DB_PASSWORD` 등 ([ENV.md](ENV.md))
4. (외부/LAN) Windows에서 **WslPortBridge**가 `0.0.0.0:8080` → WSL:8080 전달

---

## 2. 권장: 배포 스크립트로 재기동

PowerShell:

```powershell
wsl -e bash /mnt/d/ispark_prj/flowdesk/scripts/deploy-wsl.sh
```

WSL:

```bash
cd /mnt/d/ispark_prj/flowdesk
bash scripts/deploy-wsl.sh
```

하는 일: git pull(develop) → `mvn clean package` → Tomcat shutdown → WAR 복사 → startup → `login.html` 200 확인

옵션:

```bash
SKIP_GIT=1 bash scripts/deploy-wsl.sh      # pull 생략
SKIP_BUILD=1 bash scripts/deploy-wsl.sh    # 이미 빌드된 WAR만 배포
```

---

## 3. Tomcat만 수동 조작

```bash
# 상태 확인
pgrep -a java || echo "Tomcat/Java 없음"
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/flowdesk/login.html

# 중지
~/tomcat/bin/shutdown.sh
# 안 죽으면
# pkill -f catalina || true

# 시작 (SSH/세션 끊겨도 유지하려면 nohup)
nohup ~/tomcat/bin/catalina.sh start > ~/tomcat/logs/agent-start.log 2>&1 &
sleep 8
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/flowdesk/login.html
```

정상: `login.html` → **200** · 미로그인 `home.html` → **302**

로그:

```bash
tail -100 ~/tomcat/logs/catalina.out
tail -50 ~/tomcat/logs/localhost_access_log.$(date +%Y-%m-%d).txt
```

---

## 4. MySQL

```bash
sudo service mysql status
sudo service mysql start    # 필요 시
```

접속은 `127.0.0.1:3306` (외부 바인딩하지 않는 구성이 안전).

---

## 5. Windows 포트 브릿지 (LAN/외부)

WSL의 Tomcat은 Windows에서 바로 안 보일 수 있습니다.

```powershell
# 브릿지 실행 (예시 경로)
D:\ispark_prj\flowdesk\scripts\WslPortBridge\WslPortBridge.exe
# 또는
powershell -ExecutionPolicy Bypass -File D:\ispark_prj\flowdesk\scripts\ensure-wsl-port-bridge.ps1
```

방화벽(집 LAN만 권장):

```powershell
# Private만 허용
powershell -ExecutionPolicy Bypass -File D:\ispark_prj\flowdesk\scripts\open-firewall.ps1

# 8080 규칙 제거
powershell -ExecutionPolicy Bypass -File D:\ispark_prj\flowdesk\scripts\close-firewall-8080.ps1
```

---

## 6. 로컬 개발 (내장 실행, H2)

Tomcat 없이 IDE/Maven만:

```bash
cd /mnt/d/ispark_prj/flowdesk   # 또는 D:\ispark_prj\flowdesk
mvn -DskipTests spring-boot:run
```

→ `http://localhost:8080/` (context `/flowdesk` 없음)

---

## 7. 장애 체크리스트

| 증상 | 확인 |
|------|------|
| curl 000 / 연결 거부 | Java/Tomcat 기동 여부, `ss -lntp \| grep 8080` |
| 404 `/flowdesk` | WAR 전개 폴더 `~/tomcat/webapps/flowdesk` |
| 500 / DB 오류 | `setenv.sh`의 `CX_DB_*`, MySQL 상태, `catalina.out` |
| Windows만 안 됨 | WslPortBridge·방화벽 8080 |
| 업로드 깨짐 | `FLOWDESK_DATA_DIR` 절대경로 |

게스트 확인: `guest` / `guest`
