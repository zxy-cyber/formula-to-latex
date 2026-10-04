# 在开发机上跑「验收自检」：用真实 Blink 内核（和 Android WebView 同源）
# 加载与 App 完全一致的页面，模拟「点按钮 + 系统输入法打字 / 点历史条目」，逐条核对结果。
#
#   pwsh -NoProfile -File _test/run-selftest.ps1
#
# 注意：这跟 App 运行时无关，只是开发期的自动化测试，不参与 APK 打包。

$ErrorActionPreference = 'Stop'

$here = $PSScriptRoot
$profileDir = Join-Path $here 'profile'
$domFile = Join-Path $here 'dom.txt'
$errFile = Join-Path $here 'dom.err.txt'

# 找一个 Chromium 内核浏览器
$candidates = @(
  "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe",
  "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
  "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
  "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
  "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe"
) | Where-Object { $_ -and (Test-Path $_) }

if (-not $candidates) {
  Write-Host '没有找到 Edge/Chrome，无法运行自检。' -ForegroundColor Red
  exit 2
}
# 注意用 @() 包一层：只有一个候选时管道返回的是字符串，直接 [0] 只会取到第一个字符
$browser = @($candidates)[0]
Write-Host "浏览器: $browser"

# 要跑的用例页（每页自己输出 <pre id="results">…SUMMARY passed=… failed=…</pre>）
$cases = @(
  @{ Name = '编辑区与按键';   Page = 'acceptance.html' },
  @{ Name = '公式历史弹层';   Page = 'history-acceptance.html' }
)

$totalPassed = 0
$totalFailed = 0
$ranAny = $false

foreach ($case in $cases) {
  $url = ([uri](Join-Path $here $case.Page)).AbsoluteUri + '?selftest=1'
  Write-Host ''
  Write-Host ("== " + $case.Name + "  (" + $case.Page + ") ==") -ForegroundColor Cyan

  $browserArgs = @(
    '--headless=new', '--no-sandbox', '--disable-gpu', '--no-first-run',
    '--no-default-browser-check', '--disable-extensions',
    "--user-data-dir=$profileDir", '--window-size=430,900',
    '--allow-file-access-from-files', '--virtual-time-budget=12000',
    '--dump-dom', $url
  )

  Start-Process -FilePath $browser -ArgumentList $browserArgs -NoNewWindow -Wait `
    -RedirectStandardOutput $domFile -RedirectStandardError $errFile | Out-Null

  $dom = Get-Content $domFile -Raw -Encoding UTF8
  $match = [regex]::Match($dom, '(?s)<pre id="results">(.*?)</pre>')
  if (-not $match.Success) {
    Write-Host '这一页没有拿到自检结果，stderr 前 15 行：' -ForegroundColor Red
    Get-Content $errFile -ErrorAction SilentlyContinue | Select-Object -First 15
    continue
  }

  $report = $match.Groups[1].Value.Trim()
  $report -split "`n" | Where-Object { $_ -notmatch '^DIAG' } | ForEach-Object { Write-Host $_ }

  if ($report -match 'passed=(\d+)\s+failed=(\d+)') {
    $totalPassed += [int]$Matches[1]
    $totalFailed += [int]$Matches[2]
    $ranAny = $true
  }
}

Remove-Item $domFile, $errFile -ErrorAction SilentlyContinue

Write-Host ''
if (-not $ranAny) {
  Write-Host '没有跑到任何用例。' -ForegroundColor Red
  exit 2
}
if ($totalFailed -gt 0) {
  Write-Host "自检未通过：passed=$totalPassed failed=$totalFailed" -ForegroundColor Red
  exit 1
}
Write-Host "全部通过：passed=$totalPassed failed=0" -ForegroundColor Green
exit 0
