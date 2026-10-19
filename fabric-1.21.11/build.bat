@echo off
setlocal
echo [Vein Farming Fabric 1.21.11] Building mod jar...
call gradlew.bat build --no-daemon

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Mod built successfully! Output located in build\libs\
pause

