# 관리자 PowerShell에서 실행
# CX 트래커 포트 8080 인바운드 허용 (외부 접근용)

$ruleName = "CX-Inquiry-Tracker-8080"
$existing = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
if ($existing) {
  Write-Host "이미 규칙이 있습니다: $ruleName"
} else {
  New-NetFirewallRule -DisplayName $ruleName `
    -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080 `
    -Profile Any | Out-Null
  Write-Host "방화벽 규칙 추가됨: $ruleName (TCP 8080)"
}

Write-Host ""
Write-Host "접속 URL 예시:"
Get-NetIPAddress -AddressFamily IPv4 | Where-Object {
  $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*"
} | ForEach-Object {
  Write-Host ("  http://{0}:8080" -f $_.IPAddress)
}
