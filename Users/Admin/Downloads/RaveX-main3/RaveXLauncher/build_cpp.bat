@echo off
echo Building RaveX Launcher (C++)...

REM Create build directory
if not exist build mkdir build
cd build

REM Generate project files with CMake
cmake .. -G "MinGW Makefiles"
if %errorlevel% neq 0 (
    echo CMake generation failed
    cd ..
    pause
    exit /b 1
)

REM Build
cmake --build . --config Release
if %errorlevel% neq 0 (
    echo Build failed
    cd ..
    pause
    exit /b 1
)

cd ..
echo Build complete! Executable is in build directory.
pause
