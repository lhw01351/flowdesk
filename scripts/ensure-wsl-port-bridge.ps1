# 브리지 프로세스가 죽어 있으면 재기동만 함 (portproxy 사용 안 함)
$ErrorActionPreference = "Continue"
$Log = "d:\ispark_prj\flowdesk\scripts\WslPortBridge\watchdog.log"
$exe = "d:\ispark_prj\flowdesk\scripts\WslPortBridge\WslPortBridge.exe"
$LanUrl = "http://192.168.0.102:8080/flowdesk/"

function Write-Log($m) {
  Add-Content $Log "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $m" -Encoding UTF8
}

function Test-Lan {
  try {
    $r = Invoke-WebRequest $LanUrl -UseBasicParsing -TimeoutSec 3
    return ($r.StatusCode -eq 200)
  } catch { return $false }
}

if (-not (Test-Path $exe)) { Write-Log "NO_EXE"; exit 1 }

$proc = Get-Process WslPortBridge -ErrorAction SilentlyContinue
$lanOk = Test-Lan

if ($proc -and $lanOk) {
  Write-Log "OK pid=$($proc.Id)"
  exit 0
}

# portproxy가 다시 생겨 8080을 먹으면 제거
$pp = netsh interface portproxy show all | Out-String
if ($pp -match '8080') {
  Write-Log "CLEAR portproxy"
  netsh interface portproxy reset | Out-Null
  try { Restart-Service iphlpsvc -Force -ErrorAction SilentlyContinue } catch {}
  Start-Sleep -Seconds 1
}

if ($proc) {
  Write-Log "RESTART bridge (lan fail)"
  Stop-Process -Name WslPortBridge -Force -ErrorAction SilentlyContinue
  Start-Sleep -Seconds 1
} else {
  Write-Log "START bridge (not running)"
}

Start-Process -FilePath $exe -WindowStyle Minimized
Start-Sleep -Seconds 2
Write-Log "AFTER lan=$(Test-Lan)"
