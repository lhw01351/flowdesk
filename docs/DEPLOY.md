# FlowDesk 배포 가이드

최종 갱신: 2026-10-25  
대상: Windows + WSL2 Ubuntu Tomcat

## 아키텍처 (한 줄)

```
인터넷/LAN → 공유기(선택) → Windows:8080 → WslPortBridge → WSL Tomcat:8080 → /flowdesk
                                                                      ↘ MySQL (cx_tracker)
```

## 1. WAR 빌드

```bash
cd /d D:\ispark_prj\flowdesk
mvn -DskipTests package
```

산출물: `target\flowdesk.war`

## 2. Tomcat 배포 (WSL)

```bash
# MySQL 프로필 (최초 1회)
cp /mnt/d/ispark_prj/flowdesk/scripts/tomcat-setenv-mysql.sh ~/tomcat/bin/setenv.sh
chmod +x ~/tomcat/bin/setenv.sh

~/tomcat/bin/shutdown.sh
rm -rf ~/tomcat/webapps/flowdesk ~/tomcat/webapps/flowdesk.war
# 구 WAR가 있으면 함께 제거
rm -rf ~/tomcat/webapps/cx-inquiry-tracker ~/tomcat/webapps/cx-inquiry-tracker.war

cp /mnt/d/ispark_prj/flowdesk/target/flowdesk.war ~/tomcat/webapps/
# WSL 세션이 바로 닫히면 프로세스가 죽을 수 있음 → nohup 권장
nohup ~/tomcat/bin/catalina.sh start > ~/tomcat/logs/agent-start.log 2>&1
sleep 10
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/flowdesk/login.html
```

정상 시 `login.html` → **200**, 미로그인 `home.html` → **302**.

## 3. URL

| 환경 | 예시 |
|------|------|
| WSL 로컬 | http://127.0.0.1:8080/flowdesk/ |
| Windows/LAN | http://\<PC-LAN-IP\>:8080/flowdesk/ |
| 로그인 | …/flowdesk/login.html |

기본 계정: `admin` / `cxadmin123`, `hwlee` / `hwlee1234`, `hani` / `hani1234`(01), `guest` / `guest1234`(02). 회원·메뉴 관리: admin·hwlee.

## 4. Windows → WSL 포트

WSL2는 Windows와 네트워크가 분리됩니다. **WslPortBridge**로 Windows `0.0.0.0:8080` → WSL Tomcat으로 전달합니다.

```powershell
# 빌드(최초)
cd D:\ispark_prj\flowdesk\scripts\WslPortBridge
.\build.bat   # 또는 기존 WslPortBridge.exe 사용

# 설치·시작프로그램 등록
powershell -ExecutionPolicy Bypass -File D:\ispark_prj\flowdesk\scripts\install-wsl-port-bridge.ps1
```

방화벽 8080:

```powershell
powershell -ExecutionPolicy Bypass -File D:\ispark_prj\flowdesk\scripts\open-firewall.ps1
```

공인 인터넷 접근이 필요하면 공유기에서 외부 8080 → PC LAN IP:8080 포트포워딩을 설정합니다.

## 5. 재배포 체크리스트

1. `mvn -DskipTests package`
2. Tomcat shutdown → 구 exploded 폴더/`flowdesk.war` 삭제 → 새 WAR 복사 → start
3. `/flowdesk/login.html` 200 확인
4. 로그인 후 메뉴·권한 확인 (`scripts/smoke-test.sh` 권장)
5. 응답 헤더에 `Content-Security-Policy`, `X-Frame-Options` 있는지 확인(선택)

```bash
# WSL에서 Tomcat 기동 후
bash /mnt/d/ispark_prj/flowdesk/scripts/smoke-test.sh
```

## 6. 로컬 개발 (내장 실행)

```bash
mvn -DskipTests spring-boot:run
```

기본 프로필은 H2 파일 DB(`./data/flowdesk`)입니다.  
context path 없이 `http://localhost:8080/` 로 뜹니다.
