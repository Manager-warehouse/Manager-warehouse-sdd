@echo off
echo ========================================
echo   WMS Phuc Anh - Khoi dong he thong
echo ========================================

echo.
echo [1/2] Dang khoi dong Backend (port 8081)...
start "WMS Backend" cmd /k "cd /d D:\SWP391\backend && mvnw.cmd spring-boot:run"

echo.
echo [2/2] Dang khoi dong Frontend (port 3000)...
start "WMS Frontend" cmd /k "cd /d D:\SWP391\frontend && npm run dev"

echo.
echo ========================================
echo   He thong dang khoi dong...
echo   Backend : http://localhost:8081
echo   Frontend: http://localhost:3000
echo   Swagger : http://localhost:8081/swagger-ui/index.html
echo ========================================
echo.
pause
