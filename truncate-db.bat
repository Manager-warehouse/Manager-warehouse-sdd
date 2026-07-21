@echo off
setlocal EnableDelayedExpansion

set "ROOT_DIR=%~dp0"
set "SQL_FILE=%ROOT_DIR%backend\src\main\resources\db\truncate_db.sql"
set "TARGET_PARAM=%~1"

echo ===================================================
echo   XOA ^& DONG BO LAI DU LIEU KHO (DATABASE TRUNCATE)
echo ===================================================
echo.

:: Doc file env
set "ENV_FILE="
if exist "%ROOT_DIR%.env" (
    set "ENV_FILE=%ROOT_DIR%.env"
) else if exist "%ROOT_DIR%.env.supabase" (
    set "ENV_FILE=%ROOT_DIR%.env.supabase"
)

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

if defined SPRING_DATASOURCE_USERNAME set "DB_USER=!SPRING_DATASOURCE_USERNAME!"
if defined SPRING_DATASOURCE_PASSWORD set "DB_PASSWORD=!SPRING_DATASOURCE_PASSWORD!"

if not defined DB_USER set "DB_USER=postgres"
if not defined DB_PASSWORD set "DB_PASSWORD=postgres"
set "PGPASSWORD=!DB_PASSWORD!"

if not defined DB_HOST set "DB_HOST=localhost"
if not defined DB_PORT set "DB_PORT=5432"

:: Tu dong trich xuat port tu SPRING_DATASOURCE_URL neu co
if defined SPRING_DATASOURCE_URL (
    echo !SPRING_DATASOURCE_URL! | findstr /c:":5433/" >nul 2>&1
    if !ERRORLEVEL! EQU 0 set "DB_PORT=5433"
)

:: Neu truyen tham so 'vps' hoac '5433'
if /i "%TARGET_PARAM%"=="vps" set "DB_PORT=5433"
if "%TARGET_PARAM%"=="5433" set "DB_PORT=5433"

if not defined DB_NAME set "DB_NAME=postgres"

:: Tim psql executable
set "PSQL_BIN=psql"
where psql >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if exist "C:\Program Files\PostgreSQL\18\bin\psql.exe" (
        set "PSQL_BIN="C:\Program Files\PostgreSQL\18\bin\psql.exe""
    )
)

echo Target: Host !DB_HOST!:!DB_PORT! (Database: !DB_NAME!, User: !DB_USER!)
if "!DB_PORT!"=="5433" (
    echo [MODE] DANG KET NOI QUA SSH TUNNEL DEN VPS (PORT 5433)
) else (
    echo [MODE] DANG KET NOI POSTGRES LOCAL (PORT 5432)
)
echo.

!PSQL_BIN! -h !DB_HOST! -p !DB_PORT! -U !DB_USER! -d !DB_NAME! -f "%SQL_FILE%"

if %ERRORLEVEL% NEQ 0 (
    echo [INFO] Connections with !DB_USER! failed, trying fallback as postgres user...
    set "PGPASSWORD=postgres"
    !PSQL_BIN! -h !DB_HOST! -p !DB_PORT! -U postgres -d !DB_NAME! -f "%SQL_FILE%"
)

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Database truncated and re-seeded successfully on port !DB_PORT!!
) else (
    echo.
    echo [ERROR] Failed to execute database truncate. Please check PostgreSQL connection.
)

pause
