@echo off
echo ===================================================
echo   KHOI DONG HE THONG WMS (BACKEND ^& FRONTEND)
echo ===================================================

echo [1/2] Dang chay Backend (Spring Boot)...
start "WMS Backend" cmd /k "cd backend && mvn spring-boot:run"

echo [2/2] Dang chay Frontend (Vite)...
start "WMS Frontend" cmd /k "cd /d "%~dp0frontend" && npm run dev"

echo ===================================================
echo Backend va Frontend dang duoc khoi dong trong 2 cua so rieng biet.
echo Vui long kiem tra cac cua so moi mo de xem log chi tiet.
echo ===================================================
pause
