@echo off
setlocal EnableDelayedExpansion

set "ROOT_DIR=%~dp0"
set "BACKEND_DIR=%ROOT_DIR%backend"
set "FRONTEND_DIR=%ROOT_DIR%frontend"
set "ENV_FILE=%ROOT_DIR%.env"

echo ===================================================
echo   KHOI DONG WMS LOCAL VOI DATABASE TREN VPS
echo ===================================================
echo Root: %ROOT_DIR%
echo.

if exist "!ENV_FILE!" (
    echo [INFO] Nap cau hinh tu .env
    for /f "usebackq tokens=1,* delims==" %%a in ("!ENV_FILE!") do (
        set "line=%%a"
        if not "!line:~0,1!" == "#" (
            if not "%%a" == "" (
                set "%%a=%%b"
            )
        )
    )
) else (
    echo [WARN] Khong tim thay .env. Backend se dung username/password mac dinh neu chua set bien moi truong.
)

if not defined LOCAL_DB_PORT set "LOCAL_DB_PORT=5433"
if not defined DB_NAME set "DB_NAME=wms"
if not defined SPRING_DATASOURCE_USERNAME (
    if defined DB_USER (
        set "SPRING_DATASOURCE_USERNAME=%DB_USER%"
    ) else (
        set "SPRING_DATASOURCE_USERNAME=wms_user"
    )
)
if not defined SPRING_DATASOURCE_PASSWORD (
    if defined DB_PASSWORD set "SPRING_DATASOURCE_PASSWORD=%DB_PASSWORD%"
)
set "SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:%LOCAL_DB_PORT%/%DB_NAME%"
if not defined PORT set "PORT=8080"

echo [INFO] Mo cua so SSH tunnel toi VPS...
start "WMS SSH Tunnel" cmd /k "cd /d ""%ROOT_DIR%"" && call start-ssh-tunnel.bat"
echo.
echo Sau khi cua so SSH Tunnel hoi password, hay nhap password SSH.
echo Khi thay tunnel dang giu ket noi, quay lai cua so nay va nhan Enter de chay App.
pause

echo [INFO] SPRING_DATASOURCE_URL=%SPRING_DATASOURCE_URL%
echo [INFO] SPRING_DATASOURCE_USERNAME=%SPRING_DATASOURCE_USERNAME%
if not defined SPRING_DATASOURCE_PASSWORD (
    echo [ERROR] Thieu SPRING_DATASOURCE_PASSWORD hoac DB_PASSWORD trong .env.
    echo [ERROR] Hay dung password cua PostgreSQL container tren VPS, khong phai password SSH.
    pause
    exit /b 1
)
echo.

echo [1/2] Dang chay Backend (Spring Boot)...
start "WMS Backend" cmd /k "cd /d ""%BACKEND_DIR%"" && mvn spring-boot:run"

echo [2/2] Dang chay Frontend (Vite)...
start "WMS Frontend" cmd /k "cd /d ""%FRONTEND_DIR%"" && npm run dev"

echo ===================================================
echo Backend, Frontend va SSH Tunnel dang chay trong cac cua so rieng.
echo Backend: http://127.0.0.1:%PORT%
echo DB qua tunnel: jdbc:postgresql://localhost:%LOCAL_DB_PORT%/%DB_NAME%
echo ===================================================
pause
