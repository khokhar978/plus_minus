@echo off
setlocal enabledelayedexpansion
title PlusMinus - One-Time Tunnel Setup

cls
echo.
echo  ============================================================
echo       PlusMinus: One-Time Cloudflare Tunnel Setup
echo  ============================================================
echo   Run this ONCE on each new PC.
echo   After this, just double-click start.bat every time.
echo  ============================================================
echo.
echo  Target domain : game.khokhar.in.net
echo  Tunnel name   : game-tunnel
echo.

:: Always run from the folder this bat file is in
cd /d "%~dp0"

:: ============================================================
:: STEP 1 — Find cloudflared
:: ============================================================
echo [Step 1/3]  Looking for cloudflared...
echo.

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

:: cloudflared NOT found
echo  [ERROR] cloudflared was NOT found on this PC.
echo.
echo  You need to install cloudflared first.
echo.
echo  Option A - Automatic install (recommended):
echo    1. Open PowerShell as normal user (not admin)
echo    2. Run: winget install --id Cloudflare.cloudflared
echo    3. Close and re-open this window
echo.
echo  Option B - Manual:
echo    1. Download cloudflared-windows-amd64.exe from:
echo         https://github.com/cloudflare/cloudflared/releases/latest
echo    2. Rename the file to: cloudflared.exe
echo    3. Copy it into this folder:
echo         %~dp0
echo    4. Double-click setup-tunnel.bat again
echo.
goto :fatal_error

:cloudflared_found
echo  [OK] Found cloudflared at: !CLOUDFLARED_CMD!
echo.

:: ============================================================
:: STEP 2 — Cloudflare Login
:: ============================================================
echo [Step 2/3]  Cloudflare account authorization
echo.
echo  A web browser will open asking you to log into Cloudflare.
echo  When the page loads:
echo    1. Log in with your Cloudflare account
echo    2. Select the domain:  khokhar.in.net
echo    3. Click "Authorize"
echo.
echo  Press any key to open the login page...
pause >nul
echo.

"!CLOUDFLARED_CMD!" tunnel login
if errorlevel 1 (
    echo.
    echo  [ERROR] Cloudflare login FAILED or was cancelled.
    echo  Please run setup-tunnel.bat again and complete the login.
    echo.
    goto :fatal_error
)
echo.
echo  [OK] Cloudflare login successful!
echo.

:: ============================================================
:: STEP 3a — Create the tunnel
:: ============================================================
echo [Step 3/3]  Creating tunnel "game-tunnel"...
echo.

"!CLOUDFLARED_CMD!" tunnel create game-tunnel
if errorlevel 1 (
    echo.
    echo  [NOTE] The above error may mean "game-tunnel" already exists.
    echo  That is perfectly fine — continuing to DNS routing step.
)
echo.

:: ============================================================
:: STEP 3b — Route DNS
:: ============================================================
echo  Routing DNS: game.khokhar.in.net --^> game-tunnel
echo.

"!CLOUDFLARED_CMD!" tunnel route dns game-tunnel game.khokhar.in.net
if errorlevel 1 (
    echo.
    echo  [NOTE] The above error may mean the DNS record already exists.
    echo  That is perfectly fine — the domain is already routed.
)
echo.

:: ============================================================
:: Done!
:: ============================================================
echo  ============================================================
echo                    SETUP COMPLETE!
echo  ============================================================
echo.
echo  The tunnel "game-tunnel" is now linked to:
echo    https://game.khokhar.in.net
echo.
echo  You do NOT need to run this setup script again on this PC.
echo  Cloudflare credentials are saved at:
echo    %USERPROFILE%\.cloudflared\
echo.
echo  To start the game anytime, double-click:
echo    start.bat
echo.
echo  ============================================================
echo.
pause
exit /b 0

:fatal_error
echo.
echo  ============================================================
echo   Setup could not complete. Fix the issue above and retry.
echo  ============================================================
echo.
pause
exit /b 1
