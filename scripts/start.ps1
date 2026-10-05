# Builds and starts the XML demo (Tomcat via Cargo) and waits until it is healthy.
# Options: -Port 8081  -TimeoutSec 300
param(
    [int]$Port = 8081,
    [int]$TimeoutSec = 300
)
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root
$pidFile = Join-Path $root ".app.pid"
$logDir = Join-Path $root "logs"
$log = Join-Path $logDir "app.log"
$errLog = Join-Path $logDir "app.err.log"
New-Item -ItemType Directory -Force $logDir | Out-Null

function Ok($m)   { Write-Host "[ OK ] $m" -ForegroundColor Green }
function Info($m) { Write-Host "[INFO] $m" -ForegroundColor Cyan }
function Fail($m) { Write-Host "[FAIL] $m" -ForegroundColor Red; exit 1 }

# ---- 1. Java 17+ ----
function Find-Java {
    $cands = @()
    if ($env:JAVA_HOME) { $cands += (Join-Path $env:JAVA_HOME "bin\java.exe") }
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) { $cands += $cmd.Source }
    $cands += (Get-ChildItem "$env:USERPROFILE\.jdks\*\bin\java.exe" -ErrorAction SilentlyContinue | ForEach-Object FullName)
    foreach ($c in $cands) {
        if (-not (Test-Path $c)) { continue }
        $v = (& cmd /c "`"$c`" -version 2>&1" | Select-Object -First 1)
        $m = [regex]::Match($v, '"(\d+)')
        if ($m.Success -and [int]$m.Groups[1].Value -ge 17) { return @{ Path = $c; Version = $v } }
    }
    return $null
}
$java = Find-Java
if (-not $java) { Fail "JDK 17 or newer not found." }
Ok "Java: $($java.Version.Trim())"
$env:JAVA_HOME = Split-Path (Split-Path $java.Path -Parent) -Parent

# ---- 2. already running? ----
if (Test-Path $pidFile) {
    $old = [int](Get-Content $pidFile)
    if (Get-Process -Id $old -ErrorAction SilentlyContinue) { Fail "Already running (PID $old). Run stop.cmd first, or open http://localhost:$Port/spring-hibernate-xml-demo/students" }
    Remove-Item $pidFile -Force
}
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
    Fail "Port $Port is already in use by another program. Run stop.cmd, or start with -Port 8082"
}

# ---- 3. build + run Tomcat (first run downloads Maven deps and Tomcat) ----
Info "Building and starting Tomcat (first run downloads dependencies, this can take a few minutes) ..."
$mvnArgs = "/c `"`"$root\mvnw.cmd`" -B clean package cargo:run -Dcargo.servlet.port=$Port`""
$p = Start-Process cmd.exe -ArgumentList $mvnArgs -WorkingDirectory $root -PassThru -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError $errLog
$p.Id | Set-Content $pidFile
Info "Started PID $($p.Id); waiting for health (up to $TimeoutSec s) ..."

# ---- 4. health check ----
$base = "http://localhost:$Port/spring-hibernate-xml-demo"
$deadline = (Get-Date).AddSeconds($TimeoutSec)
$healthy = $false
while ((Get-Date) -lt $deadline) {
    if ($p.HasExited) { break }
    try {
        $r = Invoke-WebRequest "$base/db-check" -UseBasicParsing -TimeoutSec 3
        if ($r.Content -like "OK*") { $healthy = $true; break }
    } catch { }
    Start-Sleep -Seconds 3
}
if (-not $healthy) {
    Write-Host "`n--- last log lines ---" -ForegroundColor Yellow
    Get-Content $log -Tail 40 -ErrorAction SilentlyContinue
    Get-Content $errLog -Tail 10 -ErrorAction SilentlyContinue
    if (-not $p.HasExited) { & taskkill /PID $p.Id /T /F | Out-Null }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    Fail "Application did not become healthy. Full log: logs\app.log"
}
Ok "DB check: $($r.Content.Trim())"

# ---- 5. smoke check ----
try { $s = Invoke-WebRequest "$base/students" -UseBasicParsing -TimeoutSec 20; Ok "/students HTTP $($s.StatusCode)" }
catch { Write-Host "[FAIL] /students $($_.Exception.Message)" -ForegroundColor Red }

Write-Host ""
Write-Host "XML demo is running" -ForegroundColor Green
Write-Host "  App        $base/students"
Write-Host "  DB check   $base/db-check"
Write-Host "  Log        $log"
Write-Host "  Stop       stop.cmd"
