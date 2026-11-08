# 관리자 PowerShell에서 실행
# FlowDesk 포트 8080 인바운드 — 기본은 Private(집/회사 LAN)만 허용
# 공인 인터넷에 열지 마세요. 외부 공개가 필요하면 -Public 옵션을 명시적으로 사용.

param(
  [switch]$Public
)

$ruleName = "FlowDesk-8080"
$existing = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
if ($existing) {
  Write-Host "이미 규칙이 있습니다: $ruleName (프로필/액션을 확인하세요)"
  Get-NetFirewallRule -DisplayName $ruleName | Format-Table DisplayName, Enabled, Profile, Action
  exit 0
}

$profile = if ($Public) { "Any" } else { "Private" }

New-NetFirewallRule -DisplayName $ruleName `
  -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080 `
  -Profile $profile | Out-Null

Write-Host "방화벽 규칙 추가됨: $ruleName (TCP 8080, Profile=$profile)"
if (-not $Public) {
  Write-Host "Public 프로필에는 열리지 않습니다. (권장)"
}

Write-Host ""
Write-Host "접속 URL 예시:"
Get-NetIPAddress -AddressFamily IPv4 | Where-Object {
  $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*"
} | ForEach-Object {
  Write-Host ("  http://{0}:8080/flowdesk/login.html" -f $_.IPAddress)
}
