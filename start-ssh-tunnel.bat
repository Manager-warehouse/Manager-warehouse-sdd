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
if not defined VPS_USER set "VPS_USER=phuong"
if not defined VPS_KEY_PATH set "VPS_KEY_PATH=%USERPROFILE%\.ssh\id_ed25519"

echo =========================================================
echo   KHOI DONG SSH TUNNEL POSTGRESQL (VPS -^> LOCAL 5433)
echo =========================================================
echo Target VPS: %VPS_HOST% (User: %VPS_USER%)
echo Port Forwarding: Local 5433 -^> VPS 127.0.0.1:5432
echo SSH Key Path: %VPS_KEY_PATH%
echo =========================================================
echo.

echo [INFO] Dang ket noi SSH Tunnel...
echo [OK] SSH Tunnel da thiet lap! Giu cua so nay mo khi chay App.
echo ---------------------------------------------------------

ssh -i "%VPS_KEY_PATH%" -o StrictHostKeyChecking=accept-new -N -L 5433:127.0.0.1:5432 %VPS_USER%@%VPS_HOST%

pause
