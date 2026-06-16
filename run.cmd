@echo off
setlocal

set "ROOT_DIR=%~dp0"
set "BACKEND_DIR=%ROOT_DIR%backend"
set "FRONTEND_DIR=%ROOT_DIR%frontend"

echo ========================================
echo   WMS Phuc Anh - Khoi dong he thong
echo ========================================

echo.
echo [1/2] Dang khoi dong Backend (port 8080)...
start "WMS Backend" cmd /k "pushd ""%BACKEND_DIR%"" && mvn spring-boot:run"

echo.
echo [2/2] Dang khoi dong Frontend (port 3000)...
start "WMS Frontend" cmd /k "pushd ""%FRONTEND_DIR%"" && npm run dev"

echo.
echo ========================================
echo   He thong dang khoi dong...
echo   Backend : http://localhost:8080
echo   Frontend: http://localhost:3000
echo   Swagger : http://localhost:8080/swagger-ui/index.html
echo ========================================
echo.
pause
