@echo off
echo ========================================================
echo   Starting Localhost Firebase Realtime Database Server
echo ========================================================
echo Checking Node.js installation...
node -v >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js is not found in PATH.
    echo Please install Node.js from https://nodejs.org/ to run the local server.
    pause
    exit /b 1
)

echo Starting server on http://localhost:9000...
node firebase-local-server.js
pause
