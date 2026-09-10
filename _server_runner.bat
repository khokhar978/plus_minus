@echo off
title PlusMinus - Java Server (port 8887)
cd /d "%~dp0"
echo.
echo  =============================================
echo   PlusMinus Java Server - Starting...
echo  =============================================
echo.
echo  If you see errors below, Java is missing or
echo  the code was not compiled yet. Run start.bat
echo  =============================================
echo.
java -cp "Server\target\classes;Server\lib\*" com.khokhar.game.GameServer
echo.
echo  [Server] The Java server stopped.
echo  Press any key to close this window...
pause >nul
