# ============================================================================
#  Khoi dong TOAN BO du an WMS de test thu cong
#  - Backend  : Spring Boot  -> http://localhost:8081
#  - Frontend : Vite (React) -> http://localhost:3000
#
#  Cach dung: mo PowerShell tai thu muc goc D:\SWP391 roi go:
#      .\run-dev.ps1
#  Neu bi chan policy:
#      powershell -ExecutionPolicy Bypass -File .\run-dev.ps1
#
#  Script mo 2 cua so PowerShell rieng (backend + frontend).
#  Dong cua so tuong ung de tat tung service.
# ============================================================================

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

$backendDir  = Join-Path $root 'backend'
$frontendDir = Join-Path $root 'frontend'

Write-Host "============================================================"
Write-Host " Khoi dong WMS - Backend (8081) + Frontend (3000)"
Write-Host "============================================================"

# --- 1. Backend: Spring Boot ---
# mvnw.cmd bi loi trong moi truong nay (in ra help cua java), va khong co mvn tren PATH,
# nen goi Maven Wrapper qua java truc tiep (cach da chay test thanh cong).
$backendCmd = @"
Set-Location '$backendDir'
Write-Host '=== BACKEND (Spring Boot :8081) ===' -ForegroundColor Cyan
`$javaExe = if (`$env:JAVA_HOME -and (Test-Path "`$env:JAVA_HOME\bin\java.exe")) { "`$env:JAVA_HOME\bin\java.exe" } else { (Get-Command java).Source }
& `$javaExe -classpath '.mvn\wrapper\maven-wrapper.jar' "-Dmaven.multiModuleProjectDirectory=`$PWD" org.apache.maven.wrapper.MavenWrapperMain spring-boot:run
"@
Start-Process powershell -ArgumentList '-NoExit','-Command', $backendCmd

# --- 2. Frontend: cai dependency (lan dau) roi chay Vite ---
$frontendCmd = @"
Set-Location '$frontendDir'
Write-Host '=== FRONTEND (Vite :3000) ===' -ForegroundColor Green
if (-not (Test-Path '.\node_modules')) {
    Write-Host 'Cai dependency lan dau (npm install)...' -ForegroundColor Yellow
    npm install
}
npm run dev
"@
Start-Process powershell -ArgumentList '-NoExit','-Command', $frontendCmd

Write-Host ""
Write-Host "Da mo 2 cua so. Doi backend khoi dong xong (~20-40s) roi truy cap:" -ForegroundColor Yellow
Write-Host "    Frontend : http://localhost:3000" -ForegroundColor Green
Write-Host "    API docs : http://localhost:8081/swagger-ui/index.html" -ForegroundColor Green
