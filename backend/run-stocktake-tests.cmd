@echo off
REM ============================================================================
REM  Chay test cho Spec 006 - StockTake & Adjustment
REM  Cach dung: mo CMD/PowerShell trong thu muc backend roi go:
REM      run-stocktake-tests.cmd
REM  Hoac double-click file nay trong Explorer.
REM ============================================================================
setlocal

cd /d "%~dp0"

set TESTS=StockTakeServiceTest,StockTakeControllerTest

echo.
echo ============================================================
echo  Running Spec 006 tests: %TESTS%
echo ============================================================
echo.

REM --- Uu tien dung Maven Wrapper neu co ---
if exist "mvnw.cmd" (
    call mvnw.cmd test -Dtest=%TESTS%
    goto :done
)

REM --- Fallback: dung mvn neu da cai dat tren PATH ---
where mvn >nul 2>nul
if %ERRORLEVEL%==0 (
    call mvn test -Dtest=%TESTS%
    goto :done
)

echo [LOI] Khong tim thay mvnw.cmd hoac mvn tren PATH.
echo       Hay cai Maven, hoac chay bang lenh java truc tiep (xem README).
exit /b 1

:done
echo.
echo ============================================================
echo  Bao cao chi tiet: backend\target\surefire-reports\
echo ============================================================
endlocal
