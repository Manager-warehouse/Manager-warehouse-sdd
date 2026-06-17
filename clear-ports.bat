@echo off
title WMS Phuc Anh - Clear Ports and Processes
echo ===================================================
echo   GIAI PHONG CONG ^& TIEN TRINH TREO WMS PHUC ANH
echo ===================================================
echo.

set "found=0"

echo [+] Dang quet va giai phong cong 8080 (Backend)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr /C:":8080 " ^| findstr LISTENING') do (
    echo [+] Da phat hien cong 8080 dang bi su dung:
    tasklist /FI "PID eq %%a" /NH 2>nul
    taskkill /F /PID %%a >nul 2>&1
    set "found=1"
)

echo [+] Dang quet va giai phong cong 3000 (Frontend)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr /C:":3000 " ^| findstr LISTENING') do (
    echo [+] Da phat hien cong 3000 dang bi su dung:
    tasklist /FI "PID eq %%a" /NH 2>nul
    taskkill /F /PID %%a >nul 2>&1
    set "found=1"
)

if "%found%"=="0" (
    echo [+] Khong co tien trinh nao dang chiem dung cong 8080 hoac 3000.
) else (
    echo [+] Da giai phong cac cong thanh cong.
)

echo.
echo ===================================================
echo   DA GIAI PHONG CONG THANH CONG!
echo   Bay gio anh/chi co the khoi dong lai run-all.bat.
echo ===================================================
echo.
pause
