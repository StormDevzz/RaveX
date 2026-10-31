@echo off
@echo off
setlocal EnableExtensions
chcp 65001 >nul
title RaveX Windows Native Builder

set "ROOT_DIR=%~dp0"
set "BUILD_DIR=%ROOT_DIR%build\native-msvc"
set "NATIVE_DIR=%ROOT_DIR%src\main\resources\assets\ravex\natives"

cd /d "%ROOT_DIR%"

echo ============================================
echo  RaveX Windows Native Builder
echo ============================================
echo Project: %ROOT_DIR%
echo Build:   %BUILD_DIR%
echo Native:  %NATIVE_DIR%
echo.

echo [1/7] Checking CMake...
where cmake >nul 2>&1
if errorlevel 1 goto :cmake_missing
cmake --version
if errorlevel 1 goto :failed

echo.
echo [2/7] Checking CTest...
where ctest >nul 2>&1
if errorlevel 1 goto :ctest_missing
ctest --version
if errorlevel 1 goto :failed

echo.
echo [3/7] Checking MSVC...
where cl.exe >nul 2>&1
if errorlevel 1 goto :msvc_missing
where cl.exe

echo.
echo [4/7] Checking JDK...
where java.exe >nul 2>&1
if errorlevel 1 goto :java_missing
where javac.exe >nul 2>&1
if errorlevel 1 goto :jdk_missing
java -version
javac -version

echo.
echo [5/7] Cleaning the previous CMake configuration...
if exist "%BUILD_DIR%" rmdir /S /Q "%BUILD_DIR%"
if exist "%BUILD_DIR%" goto :failed
if not exist "%NATIVE_DIR%" mkdir "%NATIVE_DIR%"
if errorlevel 1 goto :failed

echo.
echo [6/7] Configuring and building native libraries...
cmake -S "%ROOT_DIR%src\main\cpp" -B "%BUILD_DIR%" ^
    -G "Visual Studio 17 2022" ^
    -A x64 ^
    -DCMAKE_SYSTEM_VERSION=10.0.19041.0 ^
    -DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded ^
    -DBUILD_LAUNCHER=ON
if errorlevel 1 goto :failed

cmake --build "%BUILD_DIR%" --config Release --parallel --verbose
if errorlevel 1 goto :failed

echo.
echo [7/7] Collecting native DLLs...
for /R "%BUILD_DIR%" %%F in (*.dll) do copy /Y "%%F" "%NATIVE_DIR%\" >nul
if not exist "%NATIVE_DIR%\*.dll" goto :failed

echo.
echo [8/8] Running native smoke tests...
ctest --test-dir "%BUILD_DIR%" -C Release --output-on-failure --no-tests=error
if errorlevel 1 goto :failed

echo.
echo Building the Java project...
call "%ROOT_DIR%gradlew.bat" build
if errorlevel 1 goto :failed

echo.
echo ============================================
echo  BUILD AND TESTS COMPLETED SUCCESSFULLY
echo ============================================
echo Native DLLs: %NATIVE_DIR%
echo Java output: %ROOT_DIR%build\libs
set "EXIT_CODE=0"
goto :finish

:cmake_missing
echo.
echo [ERROR] CMake was not found in PATH.
goto :failed

:ctest_missing
echo.
echo [ERROR] CTest was not found in PATH.
goto :failed

:msvc_missing
echo.
echo [ERROR] cl.exe was not found.
echo Run this file from Developer Command Prompt for VS 2022.
goto :failed

:java_missing
echo.
echo [ERROR] java.exe was not found.
goto :failed

:jdk_missing
echo.
echo [ERROR] javac.exe was not found. A JDK is required.
goto :failed

:failed
echo.
echo ============================================
echo  BUILD FAILED
echo ============================================
echo Error code: %ERRORLEVEL%
set "EXIT_CODE=1"

:finish
echo.
echo The window will remain open. Press any key to exit.
pause >nul
exit /b %EXIT_CODE%
