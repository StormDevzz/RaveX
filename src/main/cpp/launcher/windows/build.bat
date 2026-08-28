@echo off
setlocal

cd /d "%~dp0"

if "%1"=="clean" (
    echo Cleaning build directory...
    if exist build rmdir /s /q build
    echo Done.
    goto :eof
)

echo Configuring...
cmake -S . -B build -G "MinGW Makefiles"
if errorlevel 1 (
    echo CMake configure failed.
    exit /b 1
)

echo Building...
cmake --build build -j%NUMBER_OF_PROCESSORS%
if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

echo.
echo Build succeeded: build\kickx_launcher.exe and build\KickXSetup.exe
echo Copying to ..\..\..\..\..\build\launcher...
if not exist "..\..\..\..\..\build\launcher" mkdir "..\..\..\..\..\build\launcher"
copy /y "build\kickx_launcher.exe" "..\..\..\..\..\build\launcher\kickx_launcher.exe" >nul
if exist "build\KickXSetup.exe" copy /y "build\KickXSetup.exe" "..\..\..\..\..\build\launcher\KickXSetup.exe" >nul
if exist "build\icons" (
    if not exist "..\..\..\..\..\build\launcher\icons" mkdir "..\..\..\..\..\build\launcher\icons"
    xcopy /y /e /i "build\icons" "..\..\..\..\..\build\launcher\icons" >nul
) else if exist "icons" (
    if not exist "..\..\..\..\..\build\launcher\icons" mkdir "..\..\..\..\..\build\launcher\icons"
    xcopy /y /e /i "icons" "..\..\..\..\..\build\launcher\icons" >nul
)
if exist "build\flags" (
    if not exist "..\..\..\..\..\build\launcher\flags" mkdir "..\..\..\..\..\build\launcher\flags"
    xcopy /y /e /i "build\flags" "..\..\..\..\..\build\launcher\flags" >nul
)
echo Copied to build\launcher\kickx_launcher.exe and KickXSetup.exe

