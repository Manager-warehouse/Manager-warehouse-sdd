@echo off
echo ========================================
echo  WMS Selenium Tests - Create Account
echo ========================================
echo.

cd /d "%~dp0"

echo [1/2] Compiling tests...
call mvn test-compile -q
if %ERRORLEVEL% NEQ 0 (
    echo COMPILATION FAILED!
    pause
    exit /b 1
)

echo [2/2] Running Selenium tests...
call mvn test
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Some tests may have failed. Check test-output\CreateAccount_TestResult.xlsx
) else (
    echo.
    echo ALL TESTS COMPLETED!
)

echo.
echo Results: test-output\CreateAccount_TestResult.xlsx
echo.
pause
