# 관리자 PowerShell에서 1회 실행
# 불안정한 netsh portproxy 제거 + WslPortBridge 상시 기동 (근본 해결)

$ErrorActionPreference = "Continue"
$root = "d:\ispark_prj\flowdesk\scripts"
$cs = Join-Path $root "WslPortBridge\WslPortBridge.cs"
$exe = Join-Path $root "WslPortBridge\WslPortBridge.exe"
$csc = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
$userId = "$env:USERDOMAIN\$env:USERNAME"
$taskBridge = "CX-WSL-PortBridge"
$taskWatch = "CX-WSL-PortProxy-Watchdog"
$taskWatchLogon = "CX-WSL-PortProxy-Watchdog-Logon"

Write-Host "=== 1) Compile bridge ==="
if (-not (Test-Path $csc)) { throw "csc.exe not found" }
& $csc /nologo /optimize+ /out:$exe $cs
if (-not (Test-Path $exe)) { throw "compile failed" }
Write-Host "OK $exe"

Write-Host "=== 2) Remove flaky portproxy on 8080 ==="
netsh interface portproxy reset | Out-Null
try { Restart-Service iphlpsvc -Force } catch {}
Start-Sleep -Seconds 2

Write-Host "=== 3) Stop old portproxy watchdog tasks ==="
schtasks /Delete /TN $taskWatch /F 2>$null | Out-Null
schtasks /Delete /TN $taskWatchLogon /F 2>$null | Out-Null

Write-Host "=== 4) Kill old bridge if any ==="
Get-Process WslPortBridge -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1

Write-Host "=== 5) Register bridge task (logon + now) ==="
$cmd = Join-Path $root "run-wsl-port-bridge.cmd"
@"
@echo off
cd /d "$root\WslPortBridge"
start "" /MIN "$exe"
"@ | Set-Content $cmd -Encoding ASCII

cmd /c "schtasks /Delete /TN $taskBridge /F >nul 2>&1"
# 로그온 시 자동 실행 (사용자 세션 = WSL 접근 가능)
cmd /c "schtasks /Create /TN $taskBridge /SC ONLOGON /RU $userId /RL HIGHEST /IT /F /TR `"$cmd`""
Write-Host "task create done"

# 지금 즉시 브리지 기동
Write-Host "starting bridge process..."
Start-Process -FilePath $exe -WorkingDirectory (Split-Path $exe) -WindowStyle Minimized
Start-Sleep -Seconds 2
cmd /c "schtasks /Run /TN $taskBridge >nul 2>&1"
Start-Sleep -Seconds 1

Write-Host "=== 6) Firewall ==="
if (-not (Get-NetFirewallRule -DisplayName "CX-Inquiry-Tracker-8080" -ErrorAction SilentlyContinue)) {
  New-NetFirewallRule -DisplayName "CX-Inquiry-Tracker-8080" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080 -Profile Any | Out-Null
}

Write-Host "=== 7) Verify ==="
Write-Host "portproxy:"; netsh interface portproxy show all
Write-Host "process:"; Get-Process WslPortBridge -ErrorAction SilentlyContinue | Format-Table Id, ProcessName
foreach ($u in @("http://127.0.0.1:8080/flowdesk/", "http://192.168.0.102:8080/flowdesk/")) {
  try {
    $c = (Invoke-WebRequest $u -UseBasicParsing -TimeoutSec 5).StatusCode
    Write-Host "OK $c $u"
  } catch {
    Write-Host "FAIL $u :: $($_.Exception.Message)"
  }
}

Write-Host ""
Write-Host "DONE. Bridge replaces portproxy. Re-login also auto-starts bridge."
Write-Host "LTE: http://121.140.69.124:8080/flowdesk/"
Write-Host "Log: $root\WslPortBridge\bridge.log"
