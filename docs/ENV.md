# 환경변수 · 시크릿

비밀번호·DB 접속정보는 **Git에 올리지 않습니다.**  
로컬/서버에서만 설정하세요.

## 빠른 설정

```bash
cp .env.example .env          # 로컬 참고용 (.gitignore)
cp scripts/tomcat-setenv-mysql.sh ~/tomcat/bin/setenv.sh
# setenv.sh 에 CX_DB_PASSWORD, FLOWDESK_PASS_* 값을 채운 뒤
chmod +x ~/tomcat/bin/setenv.sh
```

## 주요 변수

| 변수 | 용도 |
|------|------|
| `CX_DB_URL` / `CX_DB_USER` / `CX_DB_PASSWORD` | MySQL (`profile=mysql`) |
| `FLOWDESK_DATA_DIR` | 채팅·프로필 업로드 절대경로 |
| `FLOWDESK_PASS_ADMIN` / `HWLEE` / `HANI` / `GUEST` | 시드 계정 비밀번호 (빈 값이면 신규 시드 생략) |

공개 데모 계정: **`guest` / `guest`** (`FLOWDESK_PASS_GUEST` 미설정 시 기본값).

자세한 키 목록은 저장소 루트 `.env.example` 을 보세요.
