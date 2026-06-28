# ============================================================================
#  Chay test cho Spec 006 - StockTake & Adjustment (PowerShell)
#  Cach dung: mo PowerShell trong thu muc backend roi go:
#      .\run-stocktake-tests.ps1
#  Neu bi chan policy, chay: powershell -ExecutionPolicy Bypass -File .\run-stocktake-tests.ps1
# ============================================================================

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$Tests = 'StockTakeServiceTest,StockTakeControllerTest'

Write-Host ""
Write-Host "============================================================"
Write-Host " Running Spec 006 tests: $Tests"
Write-Host "============================================================"
Write-Host ""

# --- 1. Uu tien Maven Wrapper ---
if (Test-Path ".\mvnw.cmd") {
    & .\mvnw.cmd test "-Dtest=$Tests"
    if ($LASTEXITCODE -eq 0) { exit 0 }
    Write-Host "[!] mvnw.cmd that bai, thu phuong an khac..." -ForegroundColor Yellow
}

# --- 2. mvn tren PATH ---
$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if ($mvn) {
    & mvn test "-Dtest=$Tests"
    if ($LASTEXITCODE -eq 0) { exit 0 }
    Write-Host "[!] mvn that bai, thu goi java truc tiep..." -ForegroundColor Yellow
}

# --- 3. Fallback: goi Maven Wrapper qua java truc tiep ---
# Tu dong tim java.exe (uu tien JAVA_HOME, sau do PATH)
$javaExe = $null
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $javaExe = "$env:JAVA_HOME\bin\java.exe"
} else {
    $j = Get-Command java -ErrorAction SilentlyContinue
    if ($j) { $javaExe = $j.Source }
}

if (-not $javaExe) {
    Write-Host "[LOI] Khong tim thay Java. Hay cai JDK 21 va set JAVA_HOME." -ForegroundColor Red
    exit 1
}

if (-not (Test-Path ".mvn\wrapper\maven-wrapper.jar")) {
    Write-Host "[LOI] Khong tim thay .mvn\wrapper\maven-wrapper.jar" -ForegroundColor Red
    exit 1
}

& $javaExe -classpath ".mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=$PWD" org.apache.maven.wrapper.MavenWrapperMain test "-Dtest=$Tests"

Write-Host ""
Write-Host "============================================================"
Write-Host " Bao cao chi tiet: backend\target\surefire-reports\"
Write-Host "============================================================"
