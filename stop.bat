@echo off
title PlusMinus - Stop All Services

cls
echo.
echo  ============================================================
echo       PlusMinus - Stopping All Services
echo  ============================================================
echo.

:: Kill cloudflared process
echo  Stopping Cloudflare tunnel...
taskkill /f /im cloudflared.exe >nul 2>&1
if errorlevel 0 (
    echo  [OK] cloudflared stopped.
) else (
    echo  [--] cloudflared was not running.
)

:: Kill anything on port 8887 (Java server)
echo  Stopping Java server (port 8887)...
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":8887"') do (
    if not "%%a"=="" taskkill /f /pid %%a >nul 2>&1
)
echo  [OK] Done.

:: Kill anything on port 7000 (React / Vite)
echo  Stopping React client (port 7000)...
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":7000"') do (
    if not "%%a"=="" taskkill /f /pid %%a >nul 2>&1
)
echo  [OK] Done.

echo.
echo  ============================================================
echo   All PlusMinus services have been stopped.
echo  ============================================================
echo.
ping 127.0.0.1 -n 4 >nul
