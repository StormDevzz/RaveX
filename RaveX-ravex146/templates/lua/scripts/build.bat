@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ============================================
echo  RaveX Lua Addon — Deploy
echo ============================================
echo.

set "SCRIPT_DIR=%~dp0"
set "TEMPLATES_DIR=%SCRIPT_DIR%.."

REM Выбор примера (по умолчанию 01_minimal)
if "%1"=="" (
    set "EXAMPLE=01_minimal"
) else (
    set "EXAMPLE=%1"
)

if not exist "%TEMPLATES_DIR%\%EXAMPLE%\main.lua" (
    echo [ERROR] Example "%EXAMPLE%" not found.
    echo.
    echo Available examples:
    for /d %%i in ("%TEMPLATES_DIR%\??_*") do echo   %%~nxi
    echo.
    echo Usage: build.bat [example_name]
    echo   Example: build.bat 01_minimal
    pause
    exit /b 1
)

set "LUA_DIR=%USERPROFILE%\.minecraft\ravex\addons\lua\%EXAMPLE%"
if not exist "!LUA_DIR!" mkdir "!LUA_DIR!"

copy /Y "%TEMPLATES_DIR%\%EXAMPLE%\main.lua" "!LUA_DIR!\" >nul

echo [OK] Deployed: %EXAMPLE%
echo [INFO] %TEMPLATES_DIR%\%EXAMPLE%\main.lua -^> !LUA_DIR!
echo.
pause
