@echo off
cd /d "%~dp0"
setlocal EnableDelayedExpansion
set "ROOT_DIR=%~dp0.."
set "ENV_FILE=%ROOT_DIR%\.env"

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
)

set SPRING_PROFILES_ACTIVE=secrets
set PORT=8080
if not defined LOCAL_DB_PORT set LOCAL_DB_PORT=5433
if not defined DB_NAME set DB_NAME=wms
if not defined SPRING_DATASOURCE_USERNAME (
    if defined DB_USER (
        set SPRING_DATASOURCE_USERNAME=%DB_USER%
    ) else (
        set SPRING_DATASOURCE_USERNAME=wms_user
    )
)
if not defined SPRING_DATASOURCE_PASSWORD (
    if defined DB_PASSWORD set SPRING_DATASOURCE_PASSWORD=%DB_PASSWORD%
)
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:%LOCAL_DB_PORT%/%DB_NAME%
echo [INFO] SPRING_DATASOURCE_URL=%SPRING_DATASOURCE_URL%
echo [INFO] SPRING_DATASOURCE_USERNAME=%SPRING_DATASOURCE_USERNAME%
if not defined SPRING_DATASOURCE_PASSWORD (
    echo [ERROR] Thieu SPRING_DATASOURCE_PASSWORD hoac DB_PASSWORD trong .env.
    echo [ERROR] Hay dung password cua PostgreSQL container tren VPS, khong phai password SSH.
    pause
    exit /b 1
)
call .\mvnw.cmd spring-boot:run
