@echo off
setlocal enabledelayedexpansion
title PlusMinus Launcher

echo ============================================================
echo                     PlusMinus Game Server
echo ============================================================
echo   Public URL:    https://game.khokhar.in.net
echo   Local UI:      http://localhost:7000
echo   WebSocket:     ws://localhost:8887
echo ============================================================
echo.

cd /d "%~dp0"

:: ------------------------------------------------------------
:: 1. Requirements Check
:: ------------------------------------------------------------
echo [1/5] Checking requirements...

:: Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Java is not installed or not in PATH!
    echo Please download and install JDK 17 or higher from:
    echo   https://adoptium.net/
    echo.
    pause
    exit /b 1
)

:: Check javac (JDK compiler)
javac -version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] javac was not found! You have JRE installed, but you need a JDK.
    echo Please download and install JDK 17 or higher from:
    echo   https://adoptium.net/
    echo.
    pause
    exit /b 1
)

:: Check Node.js
node -v >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Node.js is not installed or not in PATH!
    echo Please download and install Node.js (v18 or higher) from:
    echo   https://nodejs.org/
    echo.
    pause
    exit /b 1
)

:: Check Cloudflared
set "CLOUDFLARED_CMD="
where cloudflared >nul 2>&1
if %errorlevel% equ 0 (
    set "CLOUDFLARED_CMD=cloudflared"
) else if exist "%~dp0cloudflared.exe" (
    set "CLOUDFLARED_CMD=%~dp0cloudflared.exe"
) else if exist "%USERPROFILE%\Downloads\cloudflared.exe" (
    set "CLOUDFLARED_CMD=%USERPROFILE%\Downloads\cloudflared.exe"
) else if exist "%ProgramFiles%\cloudflared\cloudflared.exe" (
    set "CLOUDFLARED_CMD=%ProgramFiles%\cloudflared\cloudflared.exe"
) else (
    echo.
    echo [ERROR] cloudflared was not found!
    echo Please run setup-tunnel.bat first to set up your tunnel and cloudflared.
    echo Or download cloudflared.exe and put it in this folder.
    echo.
    pause
    exit /b 1
)

echo [OK] Requirements verified.
echo.

:: ------------------------------------------------------------
:: 2. Port Cleanup (Clear stale processes on 8887 and 7000)
:: ------------------------------------------------------------
echo [2/5] Cleaning up existing ports (8887, 7000)...
call :cleanup_ports
echo.

:: ------------------------------------------------------------
:: 3. Compile Java Backend
:: ------------------------------------------------------------
echo [3/5] Compiling Java backend...
if not exist "Server\target\classes" mkdir "Server\target\classes"

javac -cp "Server\lib\*" -d Server\target\classes Server\src\main\java\com\khokhar\game\*.java
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Java compilation failed! Check your code and try again.
    pause
    exit /b 1
)
echo [OK] Java backend compiled successfully.
echo.

:: ------------------------------------------------------------
:: 4. Install Frontend Dependencies (if needed)
:: ------------------------------------------------------------
echo [4/5] Checking React frontend dependencies...
if not exist "Client\node_modules" (
    echo [INFO] First-time setup: Installing npm packages...
    cd Client
    call npm install
    cd ..
    if %errorlevel% neq 0 (
        echo [ERROR] npm install failed!
        pause
        exit /b 1
    )
)
echo [OK] Frontend ready.
echo.

:: ------------------------------------------------------------
:: 5. Launch Servers & Tunnel
:: ------------------------------------------------------------
echo [5/5] Starting services...

:: Launch Java GameServer (port 8887) in minimized window
echo   - Starting Java Server on port 8887...
start "PlusMinus - Java Server (Port 8887)" /min cmd /c "cd /d "%~dp0Server" && java -cp "target\classes;lib\*" com.khokhar.game.GameServer"

:: Launch Vite Dev Server (port 7000) in minimized window
echo   - Starting React Dev Server on port 7000...
start "PlusMinus - Client (Port 7000)" /min cmd /c "cd /d "%~dp0Client" && npm run dev"

:: Wait 3 seconds for local servers to bind
timeout /t 3 /nobreak >nul

echo.
echo ============================================================
echo                   ALL SYSTEMS GO!
echo ============================================================
echo   Website:    https://game.khokhar.in.net
echo   Local:      http://localhost:7000
echo.
echo   Press Ctrl+C in this window at any time to STOP the game.
echo ============================================================
echo.
echo Connecting Cloudflare Tunnel to https://game.khokhar.in.net ...
echo.

:: Run Cloudflare Tunnel in foreground
"!CLOUDFLARED_CMD!" tunnel --protocol http2 run --url http://localhost:7000 game-tunnel

:: When tunnel stops, shut down backend and frontend
echo.
echo Tunnel stopped. Shutting down game servers...
call :cleanup_ports
echo [OK] All PlusMinus services stopped.
pause
exit /b 0

:: ------------------------------------------------------------
:: Subroutines
:: ------------------------------------------------------------
:cleanup_ports
for /f "tokens=5" %%a in ('netstat -aon 2^>nul ^| findstr ":8887 " ^| findstr "LISTENING"') do (
    taskkill /f /pid %%a >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -aon 2^>nul ^| findstr ":7000 " ^| findstr "LISTENING"') do (
    taskkill /f /pid %%a >nul 2>&1
)
goto :eof
