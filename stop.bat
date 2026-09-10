@echo off
setlocal enabledelayedexpansion
title PlusMinus Stopper

echo ============================================================
echo               PlusMinus: Stopping All Services
echo ============================================================
echo.

:: Kill processes listening on port 8887 (Java server)
echo Stopping Java Server on port 8887...
for /f "tokens=5" %%a in ('netstat -aon 2^>nul ^| findstr ":8887 "') do (
    taskkill /f /pid %%a >nul 2>&1
)

:: Kill processes listening on port 7000 (React dev server)
echo Stopping React Client on port 7000...
for /f "tokens=5" %%a in ('netstat -aon 2^>nul ^| findstr ":7000 "') do (
    taskkill /f /pid %%a >nul 2>&1
)

:: Kill any stray cloudflared instances running game-tunnel
echo Stopping Cloudflare Tunnel...
taskkill /f /im cloudflared.exe >nul 2>&1

echo.
echo ============================================================
echo [OK] All PlusMinus services have been stopped.
echo ============================================================
echo.
timeout /t 3
