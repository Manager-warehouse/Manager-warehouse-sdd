@echo off
setlocal EnableDelayedExpansion

set "ROOT_DIR=%~dp0"
set "BACKEND_DIR=%ROOT_DIR%backend"
set "FRONTEND_DIR=%ROOT_DIR%frontend"

echo ===================================================
echo   KHOI DONG HE THONG WMS (BACKEND ^& FRONTEND)
echo ===================================================
echo Root: %ROOT_DIR%
echo.

:: Xac dinh file .env de su dung (uu tien .env server, fallback sang .env.supabase)
set "ENV_FILE="
if exist "%ROOT_DIR%.env" (
    set "ENV_FILE=%ROOT_DIR%.env"
    echo [INFO] Su dung database server: .env
) else if exist "%ROOT_DIR%.env.supabase" (
    set "ENV_FILE=%ROOT_DIR%.env.supabase"
    echo [INFO] Su dung database Supabase: .env.supabase
) else (
    echo [WARN] Khong tim thay file .env hoac .env.supabase. Dung gia tri mac dinh.
)

:: Nap bien moi truong tu file .env vao process hien tai
if defined ENV_FILE (
    for /f "usebackq tokens=1,* delims==" %%a in ("!ENV_FILE!") do (
        set "line=%%a"
        if not "!line:~0,1!" == "#" (
            if not "%%a" == "" (
                set "%%a=%%b"
            )
        )
    )
)

echo [INFO] SPRING_DATASOURCE_URL=%SPRING_DATASOURCE_URL%
echo.

echo [1/2] Dang chay Backend (Spring Boot)...
start "WMS Backend" cmd /k "cd /d "%BACKEND_DIR%" && mvn spring-boot:run"

echo [2/2] Dang chay Frontend (Vite)...
start "WMS Frontend" cmd /k "cd /d "%FRONTEND_DIR%" && npm run dev"

echo ===================================================
echo Backend va Frontend dang duoc khoi dong trong 2 cua so rieng biet.
echo Vui long kiem tra cac cua so moi mo de xem log chi tiet.
echo ===================================================
pause
