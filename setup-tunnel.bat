@echo off
setlocal enabledelayedexpansion
title PlusMinus - Cloudflare Tunnel Setup (One-Time)

echo ============================================================
echo      PlusMinus: Cloudflare Tunnel One-Time Setup
echo ============================================================
echo.
echo Target Domain:  game.khokhar.in.net
echo Tunnel Name:    game-tunnel
echo.

:: 1. Check for cloudflared
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
    echo [ERROR] cloudflared was NOT found on this PC!
    echo.
    echo Please install cloudflared using ONE of the following options:
    echo   Option A (Recommended):
    echo     Open PowerShell and run:
    echo       winget install --id Cloudflare.cloudflared
    echo.
    echo   Option B (Direct download):
    echo     Download cloudflared-windows-amd64.exe from:
    echo       https://github.com/cloudflare/cloudflared/releases/latest
    echo     Rename it to cloudflared.exe and put it in this PlusMinus folder.
    echo.
    pause
    exit /b 1
)

echo [OK] Found cloudflared: "!CLOUDFLARED_CMD!"
echo.

:: 2. Cloudflare Login
echo ============================================================
echo Step 1: Cloudflare Authorization
echo ============================================================
echo A web browser will open shortly asking you to log into Cloudflare.
echo Select your domain (khokhar.in.net) and authorize.
echo.
echo Press any key when you are ready to open the login page...
pause >nul

"!CLOUDFLARED_CMD!" tunnel login
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Cloudflare login failed or authorization was cancelled.
    echo Please try running setup-tunnel.bat again.
    pause
    exit /b 1
)

echo.
echo [OK] Cloudflare login successful!
echo.

:: 3. Create Tunnel
echo ============================================================
echo Step 2: Creating Tunnel 'game-tunnel'
echo ============================================================
"!CLOUDFLARED_CMD!" tunnel create game-tunnel
if %errorlevel% neq 0 (
    echo.
    echo [NOTE] If it says the tunnel already exists, that is completely fine.
) else (
    echo [OK] Tunnel 'game-tunnel' created successfully!
)
echo.

:: 4. Route DNS
echo ============================================================
echo Step 3: Routing DNS (game.khokhar.in.net -> game-tunnel)
echo ============================================================
"!CLOUDFLARED_CMD!" tunnel route dns game-tunnel game.khokhar.in.net
if %errorlevel% neq 0 (
    echo.
    echo [NOTE] If it says the DNS record already exists, that is fine.
) else (
    echo [OK] DNS successfully routed to game.khokhar.in.net!
)
echo.

:: 5. Completion
echo ============================================================
echo                    SETUP COMPLETE!
echo ============================================================
echo Tunnel 'game-tunnel' is now linked to game.khokhar.in.net.
echo You DO NOT need to run this setup script again on this PC.
echo.
echo To start the game anytime:
echo   Double-click: start.bat
echo.
echo To stop all game servers:
echo   Double-click: stop.bat
echo ============================================================
echo.
pause
