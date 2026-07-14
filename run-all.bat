@echo off
setlocal

set "ROOT_DIR=%~dp0"
set "BACKEND_DIR=%ROOT_DIR%backend"
set "FRONTEND_DIR=%ROOT_DIR%frontend"

echo ===================================================
echo   KHOI DONG HE THONG WMS (BACKEND ^& FRONTEND)
echo ===================================================
echo Root: %ROOT_DIR%
echo.

echo [1/2] Dang chay Backend (Spring Boot)...
if exist "%BACKEND_DIR%\src\main\resources\.env.supabase" (
    echo [INFO] Dang nap bien moi truong tu .env.supabase...
    for /f "usebackq delims=" %%x in ("%BACKEND_DIR%\src\main\resources\.env.supabase") do (
        echo %%x | findstr /r "^#" >nul
        if errorlevel 1 (
            set "%%x"
        )
    )
)
start "WMS Backend" cmd /k "pushd ""%BACKEND_DIR%"" && mvn spring-boot:run"

echo [2/2] Dang chay Frontend (Vite)...
start "WMS Frontend" cmd /k "pushd ""%FRONTEND_DIR%"" && npm run dev"

echo ===================================================
echo Backend va Frontend dang duoc khoi dong trong 2 cua so rieng biet.
echo Vui long kiem tra cac cua so moi mo de xem log chi tiet.
echo ===================================================
pause
