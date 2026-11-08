# 관리자 PowerShell — FlowDesk 8080 인바운드 규칙 제거/비활성
$names = @("FlowDesk-8080", "CX-Inquiry-Tracker-8080")
foreach ($ruleName in $names) {
  $existing = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
  if ($existing) {
    Remove-NetFirewallRule -DisplayName $ruleName
    Write-Host "제거됨: $ruleName"
  } else {
    Write-Host "없음: $ruleName"
  }
}
