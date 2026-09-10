@echo off
setlocal enabledelayedexpansion
title PlusMinus Launcher

cls
echo.
echo  ============================================================
echo       PlusMinus Game Server - Starting Up
echo  ============================================================
echo   Public URL:  https://game.khokhar.in.net
echo   Local URL:   http://localhost:7000
echo  ============================================================
echo.

:: Always run from the folder this bat file is in
cd /d "%~dp0"

:: ============================================================
:: STEP 1 — Requirements Check
:: ============================================================
echo [Step 1/5]  Checking requirements...
echo.

:: Check Java runtime
java -version >nul 2>&1
if errorlevel 1 (
    echo  [ERROR] Java is NOT installed or not in your PATH.
    echo.
    echo  Please install JDK 17 or higher from:
    echo    https://adoptium.net/
    echo.
    echo  During installation, make sure to tick:
    echo    "Set JAVA_HOME variable" and "Add to PATH"
    echo.
    goto :fatal_error
)

:: Check Java compiler  (JDK vs JRE)
javac -version >nul 2>&1
if errorlevel 1 (
    echo  [ERROR] "javac" not found — you have a JRE but need a JDK.
    echo.
    echo  Please install JDK 17 or higher from:
    echo    https://adoptium.net/
    echo.
    goto :fatal_error
)

:: Check Node.js
node -v >nul 2>&1
if errorlevel 1 (
    echo  [ERROR] Node.js is NOT installed or not in your PATH.
    echo.
    echo  Please install Node.js v18 or higher from:
    echo    https://nodejs.org/
    echo.
    goto :fatal_error
)

:: Check npm (should come with Node.js but verify)
call npm -v >nul 2>&1
if errorlevel 1 (
    echo  [ERROR] npm is NOT found.
    echo  Reinstall Node.js from https://nodejs.org/
    echo.
    goto :fatal_error
)

:: Check cloudflared — search local folder first, then system locations
set "CLOUDFLARED_CMD="

if exist "%~dp0cloudflared.exe" (
    set "CLOUDFLARED_CMD=%~dp0cloudflared.exe"
    goto :cloudflared_found
)

where cloudflared >nul 2>&1
if not errorlevel 1 (
    set "CLOUDFLARED_CMD=cloudflared"
    goto :cloudflared_found
)

if exist "%USERPROFILE%\Downloads\cloudflared.exe" (
    set "CLOUDFLARED_CMD=%USERPROFILE%\Downloads\cloudflared.exe"
    goto :cloudflared_found
)

if exist "%ProgramFiles(x86)%\cloudflared\cloudflared.exe" (
    set "CLOUDFLARED_CMD=%ProgramFiles(x86)%\cloudflared\cloudflared.exe"
    goto :cloudflared_found
)

if exist "%ProgramFiles%\cloudflared\cloudflared.exe" (
    set "CLOUDFLARED_CMD=%ProgramFiles%\cloudflared\cloudflared.exe"
    goto :cloudflared_found
)

:: cloudflared not found
echo  [ERROR] cloudflared was NOT found on this PC.
echo.
echo  Install it using ONE of these methods:
echo.
echo  Option A - Using Windows Package Manager (easiest):
echo    Open a new PowerShell window and run:
echo      winget install --id Cloudflare.cloudflared
echo    Then close and re-open this window.
echo.
echo  Option B - Manual download:
echo    Download "cloudflared-windows-amd64.exe" from:
echo      https://github.com/cloudflare/cloudflared/releases/latest
echo    Rename it to "cloudflared.exe" and place it in this folder:
echo      %~dp0
echo.
echo  After installing, run setup-tunnel.bat if you have not yet.
echo.
goto :fatal_error

:cloudflared_found
echo  [OK] Java (JDK)  ...... found
echo  [OK] Node.js / npm .... found
echo  [OK] cloudflared ...... found at: !CLOUDFLARED_CMD!
echo.

:: Check if tunnel credentials exist (has setup-tunnel.bat been run?)
if not exist "%USERPROFILE%\.cloudflared" (
    echo  [WARNING] No cloudflared credentials found at %USERPROFILE%\.cloudflared
    echo  It looks like you have NOT run setup-tunnel.bat yet.
    echo  Please run setup-tunnel.bat first, then re-run start.bat.
    echo.
    goto :fatal_error
)

:: ============================================================
:: STEP 2 — Clean up stale processes on ports 8887 and 7000
:: ============================================================
echo [Step 2/5]  Cleaning up old processes on ports 8887 and 7000...
call :cleanup_ports
echo  [OK] Ports cleared.
echo.

:: ============================================================
:: STEP 3 — Compile Java Backend
:: ============================================================
echo [Step 3/5]  Compiling Java backend...

if not exist "Server\lib\Java-WebSocket-1.5.6.jar" (
    echo  [ERROR] Server\lib\ folder is missing required JAR files.
    echo  The Server\lib folder must be present with the JAR dependencies.
    echo  Make sure you copied the full PlusMinus folder.
    echo.
    goto :fatal_error
)

if not exist "Server\target\classes" (
    mkdir "Server\target\classes" 2>nul
)

javac -encoding UTF-8 -cp "Server\lib\*" -d "Server\target\classes" "Server\src\main\java\com\khokhar\game\*.java"
if errorlevel 1 (
    echo.
    echo  [ERROR] Java compilation FAILED.
    echo  Check that all .java files are present in Server\src\main\java\com\khokhar\game\
    echo.
    goto :fatal_error
)
echo  [OK] Java backend compiled.
echo.

:: ============================================================
:: STEP 4 — Install frontend npm packages (first time only)
:: ============================================================
echo [Step 4/5]  Checking frontend packages...
if not exist "Client\node_modules" (
    echo  [INFO] First-time run: Installing npm packages for the React client.
    echo  This may take 1-2 minutes on first run, please wait...
    echo.
    cd /d "%~dp0Client"
    call npm install
    if errorlevel 1 (
        cd /d "%~dp0"
        echo.
        echo  [ERROR] npm install FAILED. Check your internet connection and try again.
        echo.
        goto :fatal_error
    )
    cd /d "%~dp0"
)
echo  [OK] Frontend packages ready.
echo.

:: ============================================================
:: STEP 5 — Launch both servers in separate minimized windows
:: ============================================================
echo [Step 5/5]  Starting game servers...

echo  - Launching Java Server (port 8887)...
start "PlusMinus Java Server" /min "%~dp0_server_runner.bat"

echo  - Launching React Client (port 7000)...
start "PlusMinus React Client" /min "%~dp0_client_runner.bat"

:: Wait for servers to bind before connecting the tunnel
echo.
echo  Waiting 5 seconds for servers to start...
ping 127.0.0.1 -n 6 >nul

echo.
echo  ============================================================
echo             ALL SYSTEMS GO!  Game is live at:
echo.
echo      https://game.khokhar.in.net
echo      http://localhost:7000  (local)
echo.
echo    Keep this window open. Press Ctrl+C to STOP everything.
echo    Two minimized windows run the Java server and React client.
echo  ============================================================
echo.
echo  Connecting Cloudflare Tunnel (press Ctrl+C to disconnect)...
echo.

:: Run cloudflared in the FOREGROUND — this window keeps the game alive
"!CLOUDFLARED_CMD!" tunnel --protocol http2 run --url http://localhost:7000 game-tunnel

:: Reaches here only when tunnel is stopped (Ctrl+C or error)
echo.
echo  Tunnel disconnected. Shutting down game servers...
call :cleanup_ports
echo  [OK] All PlusMinus services stopped.
echo.
pause
exit /b 0


:: ============================================================
:: Subroutines
:: ============================================================

:cleanup_ports
:: Kill anything on port 8887 (Java server)
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":8887"') do (
    if not "%%a"=="" taskkill /f /pid %%a >nul 2>&1
)
:: Kill anything on port 7000 (React client)
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":7000"') do (
    if not "%%a"=="" taskkill /f /pid %%a >nul 2>&1
)
exit /b 0

:fatal_error
echo  ============================================================
echo   Startup failed. Please fix the issue above and try again.
echo  ============================================================
echo.
pause
exit /b 1
