@echo off
setlocal EnableDelayedExpansion

set "ROOT_DIR=%~dp0"
set "ENV_FILE=%ROOT_DIR%.env"

if exist "!ENV_FILE!" (
    for /f "usebackq tokens=1,* delims==" %%a in ("!ENV_FILE!") do (
        set "line=%%a"
        if not "!line:~0,1!" == "#" (
            if not "%%a" == "" (
                set "%%a=%%b"
            )
        )
    )
)

if not defined VPS_HOST set "VPS_HOST=4.194.232.143"
if not defined VPS_USER set "VPS_USER=hoanganh"
if not defined VPS_KEY_PATH set "VPS_KEY_PATH=%USERPROFILE%\.ssh\id_ed25519"
if not defined LOCAL_DB_PORT set "LOCAL_DB_PORT=5433"
if not defined VPS_DB_HOST set "VPS_DB_HOST=127.0.0.1"
if not defined VPS_DB_PORT set "VPS_DB_PORT=5432"

echo =========================================================
echo   KHOI DONG SSH TUNNEL POSTGRESQL (VPS -^> LOCAL %LOCAL_DB_PORT%)
echo =========================================================
echo Target VPS: %VPS_HOST% (User: %VPS_USER%)
echo Port Forwarding: Local %LOCAL_DB_PORT% -^> VPS %VPS_DB_HOST%:%VPS_DB_PORT%
if exist "%VPS_KEY_PATH%" (
    echo SSH Key Path: %VPS_KEY_PATH%
) else (
    echo SSH Key Path: not found - password login will be requested
)
echo =========================================================
echo.

echo [INFO] Dang ket noi SSH Tunnel...
echo [INFO] Neu duoc hoi password, nhap mat khau SSH cua user %VPS_USER%.
echo [INFO] Khi tunnel thanh cong, giu cua so nay mo khi chay App.
echo ---------------------------------------------------------

if exist "%VPS_KEY_PATH%" (
    ssh -i "%VPS_KEY_PATH%" -o StrictHostKeyChecking=accept-new -o ExitOnForwardFailure=yes -N -L %LOCAL_DB_PORT%:%VPS_DB_HOST%:%VPS_DB_PORT% %VPS_USER%@%VPS_HOST%
) else (
    ssh -o StrictHostKeyChecking=accept-new -o ExitOnForwardFailure=yes -N -L %LOCAL_DB_PORT%:%VPS_DB_HOST%:%VPS_DB_PORT% %VPS_USER%@%VPS_HOST%
)

pause
