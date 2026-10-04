# 在开发机上跑一遍「验收自检」：用真实 Blink 内核（和 Android WebView 同源）
# 模拟「点按钮 + 系统输入法打字」，逐条核对验收标准里的 LaTeX 结果。
#
#   pwsh -NoProfile -File _test/run-selftest.ps1
#
# 注意：这跟 App 无关，只是开发期的自动化测试，不参与 APK 打包。

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$page = Join-Path $PSScriptRoot 'acceptance.html'
$profileDir = Join-Path $PSScriptRoot 'profile'
$domFile = Join-Path $PSScriptRoot 'dom.txt'
$errFile = Join-Path $PSScriptRoot 'dom.err.txt'

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

$url = ([uri]$page).AbsoluteUri + '?selftest=1'
$browserArgs = @(
  '--headless=new', '--no-sandbox', '--disable-gpu', '--no-first-run',
  '--no-default-browser-check', '--disable-extensions',
  "--user-data-dir=$profileDir", '--window-size=430,900',
  '--allow-file-access-from-files', '--virtual-time-budget=12000',
  '--dump-dom', $url
)

Write-Host "浏览器: $browser"
Write-Host "用例页: $url"

$proc = Start-Process -FilePath $browser -ArgumentList $browserArgs -NoNewWindow -Wait -PassThru `
  -RedirectStandardOutput $domFile -RedirectStandardError $errFile

$dom = Get-Content $domFile -Raw -Encoding UTF8
$match = [regex]::Match($dom, '(?s)<pre id="results">(.*?)</pre>')
if (-not $match.Success) {
  Write-Host '没有拿到自检结果，stderr 前 20 行：' -ForegroundColor Red
  Get-Content $errFile -ErrorAction SilentlyContinue | Select-Object -First 20
  exit 2
}

$report = $match.Groups[1].Value.Trim()
$report -split "`n" | Where-Object { $_ -notmatch '^DIAG' } | ForEach-Object { Write-Host $_ }

$failed = 0
if ($report -match 'failed=(\d+)') { $failed = [int]$Matches[1] }

Remove-Item $domFile, $errFile -ErrorAction SilentlyContinue

if ($failed -gt 0) {
  Write-Host "自检未通过：failed=$failed" -ForegroundColor Red
  exit 1
}
Write-Host '全部通过。' -ForegroundColor Green
exit 0
