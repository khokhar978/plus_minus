@echo off
title PlusMinus - React Client (port 7000)
cd /d "%~dp0Client"
echo.
echo  =============================================
echo   PlusMinus React Client - Starting...
echo  =============================================
echo.
echo  Starting Vite dev server on port 7000...
echo  =============================================
echo.
call npm run dev
echo.
echo  [Client] The React server stopped.
echo  Press any key to close this window...
pause >nul
