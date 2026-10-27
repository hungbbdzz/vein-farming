@echo off
setlocal
echo [Vein Farming Forge 1.20.1] Launching Minecraft Client...
call gradlew.bat runClient --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Game launch failed!
    pause
    exit /b %ERRORLEVEL%
)
pause
