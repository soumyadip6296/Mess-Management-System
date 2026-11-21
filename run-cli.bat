@echo off
echo ========================================================
echo   Starting Mess Management System CLI on Localhost
echo ========================================================

:: Check if port 9000 is already listening
netstat -ano | findstr :9000 >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Starting Localhost Firebase Server on http://localhost:9000 ...
    start "Local Firebase Server (Port 9000)" /min node firebase-local-server.js
    timeout /t 2 /nobreak >nul
) else (
    echo [OK] Localhost Firebase Server is already running on port 9000.
)

echo Launching CLI Console Application...
java -cp "bin;lib\gson-2.10.1.jar;resources" com.messmgmt.Main --console
pause
