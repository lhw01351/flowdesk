# FlowDesk 서버 환경 인수인계 / 협업용 문서

> **목적:** Ubuntu(WSL) 설치부터 외부 웹 접속까지 전체 흐름을 “이야기”로 이해하게 하고,  
> 다른 AI/개발자가 **추측 없이** 이어서 작업할 수 있게 **확인된 사실**과 **미확인**을 구분한다.  
> **작성 기준일:** 2026-11-01 (읽기 전용 조사)  
> **주의:** 비밀번호·토큰은 문서에 넣지 않음. `setenv.sh` 등 실파일에만 존재.

---

## 0. 30초 요약 (다른 AI가 먼저 읽을 것)

| 항목 | 확인된 값 |
|------|-----------|
| 실제 형태 | **Windows 호스트 + WSL2 Ubuntu** (단독 Ubuntu 서버 아님) |
| Ubuntu | 24.04.4 LTS (Noble), 커널 `microsoft-standard-WSL2` |
| WSL 계정 | `lhw` (`/home/lhw`) |
| 프로젝트 | `D:\ispark_prj\flowdesk` ↔ `/mnt/d/ispark_prj/flowdesk` |
| 스택 | Spring Boot **2.7.18** WAR + MyBatis + **MySQL(프로필 mysql)** |
| 빌드 | Maven **3.9.6** (`D:\ispark_prj\tools\apache-maven-3.9.6`) → `target/flowdesk.war` |
| Java | OpenJDK **17.0.20.1** (`JAVA_HOME` 미설정, PATH의 `/usr/bin/java`) |
| Tomcat | **9.0.117**, 경로 `/home/lhw/tomcat`, 포트 **8080** (`*:8080`) |
| 컨텍스트 | `/flowdesk` → URL `http://…:8080/flowdesk/` |
| DB | MySQL `127.0.0.1:3306` / DB명 `cx_tracker` (프로필 `mysql`) |
| 업로드 | `FLOWDESK_DATA_DIR=/mnt/d/ispark_prj/data` (chat/profile) |
| Windows LAN IP | **192.168.0.102** (Wi-Fi) |
| WSL eth0 IP | **172.30.251.3** (가변 가능) |
| 외부→WSL 브리지 | `WslPortBridge.exe` (Windows `0.0.0.0:8080` → WSL:8080) **실행 중** |
| Windows 방화벽 | 스크립트 `open-firewall.ps1` 존재. 규칙 존재 여부는 **이번 조사에서 미확인(출력 없음)** |
| 공유기 포트포워딩 | **이 PC에서 확인 불가** (공유기 관리 UI 미접근) |

**정상 서비스 URL (사내/동일 Wi-Fi 기준):**  
`http://192.168.0.102:8080/flowdesk/`

---

## ① 전체적인 구성 요약

이 PC는 “리눅스만 깔린 서버”가 아니라,

1. **Windows**가 바깥 문을 열고 (Wi-Fi IP, 방화벽, WslPortBridge)
2. 안쪽 **WSL2 Ubuntu**에서 Tomcat이 FlowDesk WAR를 돌리고
3. 데이터는 **MySQL(WSL 로컬)** + **파일 업로드 디렉터리(`/mnt/d/ispark_prj/data`)** 를 씁니다.

최종 상태: **브라우저로 FlowDesk(문의 트래커·인바디·채팅·회원관리 등)에 접속 가능한 개인/사내 웹 서비스**.

```
[외부/같은 Wi-Fi 사용자]
        │
        ▼
 Windows 192.168.0.102:8080  (WslPortBridge 리스닝)
        │
        ▼
 WSL Ubuntu Tomcat *:8080
        │
        ▼
 /flowdesk (flowdesk.war 전개)
        │
        ├─ MySQL 127.0.0.1:3306 / cx_tracker
        └─ 파일 /mnt/d/ispark_prj/data/{chat-uploads,profile-images}
```

---

## ② 작업 순서별 설명 (왜 했고, 다음에 무엇이 연결되는가)

### 1단계. Ubuntu를 “서버 OS”로 쓰기 — 실제로는 WSL2

**무엇을 했는가?**  
Windows 안에 Ubuntu 24.04를 WSL2로 설치·사용. 호스트명 `DESKTOP-MG8GQH5`, 계정 `lhw`.

**왜?**  
Tomcat·MySQL·Java 서버 작업은 Linux 환경이 익숙하고, 프로젝트 소스는 Windows 디스크(`D:\`)에 두면서 Linux에서 빌드/실행하기 좋음.  
`/mnt/d/...` 가 Windows `D:\` 공유 마운트.

**역할**  
서버 프로세스(Tomcat, MySQL)가 사는 “방”.

**다음 연결**  
방에 Java(프로그램 실행기)와 Tomcat(웹 서버)을 넣어야 함.

**현재**  
정상 (Ubuntu 24.04.4 LTS 확인).

> **전문용어**  
> - **WSL2:** Windows Subsystem for Linux. Windows 안에서 Linux를 돌리는 가상 환경.  
> - **cwd:** 프로세스의 현재 작업 폴더. 상대경로 `./data` 가 여기에 붙음.

---

### 2단계. Java / JDK

**확인된 것**

- `java` / `javac` → OpenJDK **17.0.20.1**
- 경로: `/usr/lib/jvm/java-17-openjdk-amd64`
- `JAVA_HOME` **미설정** (PATH만으로 동작 중)
- 프로젝트 `pom.xml`의 `<java.version>11</java.version>` → **11 이상이면 빌드·실행 가능** (현재 17로 동작)

**왜 필요한가?**  
FlowDesk는 Java로 작성. Tomcat도 JVM 위에서 돈다.  
JDK = 컴파일(javac) + 실행(java) 도구 묶음.

**다음 연결**  
소스 → Maven이 JDK로 컴파일 → WAR.

**현재**  
정상. (원하면 나중에 `JAVA_HOME`을 명시해도 됨 — 필수는 아님)

---

### 3단계. 프로젝트 구조와 Maven → WAR

**위치**

| Windows | WSL |
|---------|-----|
| `D:\ispark_prj\flowdesk` | `/mnt/d/ispark_prj/flowdesk` |

**프레임워크 (확인)**

- Spring Boot 2.7.18
- packaging: **war**
- MyBatis
- 내장 Tomcat은 `provided` → **외부 Tomcat에 배포**하는 형태
- 기본 yml은 H2, **실운영은 profile `mysql`**

**Maven**

- `D:\ispark_prj\tools\apache-maven-3.9.6`
- 빌드 예:  
  `/mnt/d/ispark_prj/tools/apache-maven-3.9.6/bin/mvn -DskipTests package`  
- 산출물: `flowdesk/target/flowdesk.war`

**주요 설정 파일**

| 파일 | 역할 |
|------|------|
| `pom.xml` | 의존성·WAR 이름(`flowdesk`) |
| `src/main/resources/application.yml` | 기본 설정(포트 개념·H2·업로드 경로 템플릿) |
| `src/main/resources/application-mysql.yml` | MySQL 데이터소스 (실서비스) |
| `src/main/resources/schema.sql` | 스키마 (H2/초기용; mysql 프로필은 `sql.init.mode: never`) |
| `src/main/resources/static/*` | 프론트 HTML/JS/CSS |
| `src/main/java/kr/flowdesk/**` | 백엔드 |

**왜 WAR인가?**  
WAR = Web Application Archive. Tomcat이 받아서 풀고 `/flowdesk` 앱으로 실행하는 패키지.

**다음 연결**  
WAR를 Tomcat `webapps`에 복사.

---

### 4단계. Tomcat

**확인된 것**

| 항목 | 값 |
|------|-----|
| 버전 | Apache Tomcat 9.0.117 |
| 홈 | `/home/lhw/tomcat` (`catalina.home` / `catalina.base`) |
| 기동 | `~/tomcat/bin/catalina.sh start` |
| 종료 | `~/tomcat/bin/shutdown.sh` |
| 배포 | `webapps/flowdesk.war` → 자동 전개 `webapps/flowdesk/` |
| 포트 | **8080** (프로세스 확인: `*:8080`) |
| 로그 | `/home/lhw/tomcat/logs/` (`catalina.out`, `localhost_access_log.*.txt`) |
| 런타임 옵션 | `bin/setenv.sh` |

**setenv.sh가 하는 일 (의미만, 비밀값 생략)**

- `SPRING_PROFILES_ACTIVE=mysql` → MySQL 설정 활성화
- DB 접속용 환경변수 `CX_DB_USER` / `CX_DB_PASSWORD` (값은 파일에만)
- `FLOWDESK_DATA_DIR` 기본 `/mnt/d/ispark_prj/data` → 채팅·프로필 업로드 고정

**왜 Tomcat인가?**  
Spring Boot를 “항상 켜진 웹서버”로 띄우기 위함. IDE 실행이 아니라 서버 배포.

**현재**  
Tomcat Java 프로세스 실행 중(조사 시점 pid 존재). FlowDesk 기동 로그에 profile `mysql`, 업로드 경로 고정 확인됨.

**주의 (과거 이슈 → 해결됨)**  
업로드를 `./data/...` 상대경로로 두면 Tomcat cwd가 바뀔 때 파일이 “사라진 것처럼” 보임(DB 메타만 남고 미리보기 404).  
→ `FLOWDESK_DATA_DIR` 절대 경로로 고정.

---

### 5단계. DB (MySQL)

**확인된 것**

- WSL에서 `127.0.0.1:3306` LISTEN
- 앱 프로필: `mysql` → DB `cx_tracker`
- `application-mysql.yml`의 `sql.init.mode: never` → 스키마는 별도 setup 스크립트/기존 DB 사용

**역할**  
회원·문의·채팅 메타·메뉴 권한 등 영속 데이터.

---

### 6단계. 네트워크 — “왜 포트포워딩/브리지가 필요한가?”

여기가 초보자가 가장 헷갈리는 부분이다. **층이 세 개**다.

#### (A) Ubuntu/WSL 안

- Tomcat이 `*:8080` 으로 리슨 → WSL 안에서는 `http://127.0.0.1:8080/flowdesk/` 가능.
- WSL eth0: `172.30.251.3` (Windows의 `vEthernet (WSL)` `172.30.240.1`과 같은 가상망).

#### (B) Windows ↔ WSL (필수 브리지)

WSL2 IP는 Windows LAN IP와 **다름**.  
그래서 Windows에서 `WslPortBridge`가:

`Windows 0.0.0.0:8080` → `WSL(Tomcat):8080`

으로 연결.  
스크립트: `flowdesk/scripts/WslPortBridge/`, `ensure-wsl-port-bridge.ps1`, `run-wsl-port-bridge.cmd`  
조사 시점: **WslPortBridge 프로세스 실행 중**.

LAN 접속 예 (스크립트에 명시): `http://192.168.0.102:8080/flowdesk/`

#### (C) Windows 방화벽

`open-firewall.ps1` → 인바운드 TCP **8080** 허용 규칙 추가용.  
**이번 조사:** 규칙 객체가 조회 결과에 안 보임 → **존재 여부 미확정** (관리자 권한/규칙명 변경 가능).  
같은 Wi-Fi에서 이미 접속된다면 허용된 상태일 가능성 높음.

#### (D) 공유기 포트포워딩 (인터넷 외부)

집/사무실 공유기에서 `외부포트 → 192.168.0.102:8080` 을 열어야 인터넷에서 접속 가능.  
**확인 여부: 불가 (공유기 UI/설정 미조사).** 추측하지 않음.

**ufw (Ubuntu 방화벽)**  
sudo 없이 상태 확인 실패 → **미확인**. Tomcat이 이미 `*:8080`이면 WSL 내부 방화벽이 막지 않거나 비활성일 수 있음.

---

## ③ 전체 동작 구조 (요청 한 번의 여행)

```
외부 사용자 브라우저
  → (미확인) 공유기 포트포워딩
  → Windows LAN 192.168.0.102:8080
  → Windows 방화벽 (8080 허용 가정/스크립트 존재)
  → WslPortBridge (Windows→WSL 전달)
  → WSL Tomcat :8080
  → 컨텍스트 /flowdesk (WAR)
  → Spring Controller + MyBatis
  → MySQL / 파일스토리지
  → HTML/JSON 응답
```

**같은 PC에서만 볼 때 짧은 경로**

```
브라우저 → http://127.0.0.1:8080/flowdesk/
  (Windows면 Bridge 경유, WSL 안이면 Tomcat 직행)
```

---

## ④ 현재 상태 / 주의사항

### 정상으로 확인된 것

- WSL Ubuntu + Java 17 + Maven 3.9.6
- Tomcat 9 + `flowdesk.war` 배포·기동
- MySQL 로컬 리스닝 + Spring profile `mysql`
- 업로드 경로 절대경로 고정 (`/mnt/d/ispark_prj/data/...`)
- WslPortBridge 실행, Windows Wi-Fi IP `192.168.0.102`

### 미확인 / 주의

| 항목 | 상태 | 영향 |
|------|------|------|
| 공유기 포트포워딩 | 미확인 | 인터넷 외부 접속 가능 여부 모름 |
| Windows 방화벽 규칙 실재 | 미확인 | 타 PC 접속 실패 시 의심 |
| Ubuntu ufw | 미확인 | 보통 WSL에서는 Windows 쪽이 더 중요 |
| `JAVA_HOME` | 미설정 | 지금은 PATH로 충분 |
| WSL IP 변경 | eth0 IP는 재시작 시 바뀔 수 있음 | Bridge가 WSL IP를 재해석해야 함 (Bridge 재시작 스크립트 존재) |
| setenv 비밀번호 | 파일에 평문 | 인수인계 시 파일 권한·공유 주의 (문서엔 미기재) |

### 자주 깨졌던 패턴 (이미 겪은 것)

1. **상대경로 업로드** → 미리보기 깨짐 → `FLOWDESK_DATA_DIR` 고정으로 해결  
2. **Tomcat 재배포** 후 Bridge/방화벽 미기동 → LAN 접속 안 됨  
3. **guest 계정**은 문의 트래커 본인 데이터만 (auth 02, 대상자 콤보 없음)

---

## 서버 공부 순서 (추천 로드맵)

1. **Ubuntu와 Linux 이해**  
   계정 `lhw`, `$HOME`, `/mnt/d` 마운트, 기본 명령 `ls` `cd` `ps` `ss`
2. **Java와 프로젝트 구조**  
   JDK vs JRE, `pom.xml`, Maven `package`, WAR 의미
3. **Tomcat 이해**  
   `webapps`, `catalina.sh`, `server.xml` Connector 8080, 로그
4. **네트워크와 포트포워딩**  
   WSL IP vs Windows LAN IP vs 공유기 — 층을 구분해서 보기
5. **전체 서비스 동작 확인**  
   `http://192.168.0.102:8080/flowdesk/` 로그인 → API → 업로드 파일

---

## 자주 쓰는 명령 (기능 포함)

```bash
# Tomcat 시작/종료
~/tomcat/bin/catalina.sh start
~/tomcat/bin/shutdown.sh

# WAR 빌드
cd /mnt/d/ispark_prj/flowdesk
/mnt/d/ispark_prj/tools/apache-maven-3.9.6/bin/mvn -DskipTests package

# 배포 (예시)
cp target/flowdesk.war ~/tomcat/webapps/

# 포트/프로세스 확인
ss -lntp | grep 8080
ps aux | grep catalina

# 로그
tail -f ~/tomcat/logs/catalina.out
```

Windows (관리자 PowerShell 필요할 수 있음):

```powershell
# 방화벽 8080
powershell -ExecutionPolicy Bypass -File d:\ispark_prj\flowdesk\scripts\open-firewall.ps1

# WSL 포트 브리지 감시/재시작
powershell -ExecutionPolicy Bypass -File d:\ispark_prj\flowdesk\scripts\ensure-wsl-port-bridge.ps1
```

---

## 다른 AI에게 주는 작업 규칙

1. **설정 변경 전** 이 문서의 “확인됨/미확인”을 먼저 보라.  
2. 비밀번호를 채팅/문서에 다시 쓰지 말 것. `setenv.sh` / 환경변수만 사용.  
3. 업로드 경로는 **상대경로로 되돌리지 말 것**.  
4. 외부 접속 문제는 먼저 `WslPortBridge` → Windows 방화벽 → (그다음) 공유기를 보라.  
5. 코드/배포 변경 후 검증 URL: `http://127.0.0.1:8080/flowdesk/` 및 LAN IP.

---

## 관련 경로 빠른 인덱스

```
D:\ispark_prj\flowdesk\                 # 소스
D:\ispark_prj\tools\apache-maven-3.9.6\ # Maven
D:\ispark_prj\data\                     # 업로드 영속 데이터
\\wsl$\...\home\lhw\tomcat\             # Tomcat (WSL)
flowdesk\scripts\WslPortBridge\         # Windows↔WSL 8080 브리지
flowdesk\scripts\tomcat-setenv-mysql.sh # setenv 원본
flowdesk\docs\SERVER_ENV_HANDOFF.md     # 이 문서
```

---

*이 문서는 2026-11-01 읽기 전용 조사 결과입니다. 시간이 지나면 WSL IP·프로세스 PID·방화벽 규칙은 달라질 수 있으니, 중요 작업 전 `ss`/`Get-Process WslPortBridge`로 재확인하세요.*
