@echo off
echo ========================================================
echo   Compiling Mess Management System (Java + Firebase Cloud)
echo ========================================================

if not exist "bin" mkdir "bin"

echo Compiling Java source files with Firebase libraries...
javac -cp "lib\gson-2.10.1.jar;resources" -d bin src\com\messmgmt\*.java src\com\messmgmt\config\*.java src\com\messmgmt\model\*.java src\com\messmgmt\dao\*.java src\com\messmgmt\util\*.java src\com\messmgmt\ui\*.java

if %ERRORLEVEL% EQU 0 (
    echo [OK] Compilation successful!
    echo Copying properties and data files...
    copy resources\firebase.properties bin\firebase.properties >nul
    if exist "resources\firebase-data.json" copy resources\firebase-data.json bin\firebase-data.json >nul
    echo Build complete. You can now run "run.bat" or "run-cli.bat".
) else (
    echo [ERROR] Compilation failed. Please check Java JDK installation.
)
pause
