@echo off
chcp 65001 >nul
title RaveX Windows Native Builder

echo ============================================
echo  RaveX - Сборка нативных библиотек для Windows
echo ============================================
echo.

REM Проверка наличия CMake
where cmake >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ОШИБКА] CMake не найден! Установите CMake 3.16+ и добавьте в PATH.
    echo   Скачать: https://cmake.org/download/
    pause
    exit /b 1
)

REM Проверка наличия компилятора
where cl.exe >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    where g++.exe >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo [ОШИБКА] Компилятор C++ не найден!
        echo   Установите MSVC (Visual Studio Build Tools) или MinGW-w64.
        echo.
        echo   Для MSVC: укажите путь в переменной CC и CXX, либо запустите из
        echo     "Developer Command Prompt for VS"
        echo   Для MinGW: установите из https://winlibs.com/
        pause
        exit /b 1
    ) else (
        echo [INFO] Найден GCC (MinGW): 
        where g++.exe
    )
) else (
    echo [INFO] Найден MSVC: 
    where cl.exe
)

echo.
echo [1/5] Создание директории сборки...
if not exist "build\native" mkdir "build\native"

echo.
echo [2/5] Конфигурация CMake...
cd build\native

cmake ..\..\src\main\cpp ^
    -DCMAKE_BUILD_TYPE=Release ^
    -DCMAKE_INSTALL_PREFIX=..\..\src\main\resources\assets\ravex\natives

if %ERRORLEVEL% NEQ 0 (
    echo [ОШИБКА] Конфигурация CMake не удалась!
    cd ..\..
    pause
    exit /b 1
)

echo.
echo [3/5] Сборка нативных библиотек...
cmake --build . --config Release --parallel

if %ERRORLEVEL% NEQ 0 (
    echo [ОШИБКА] Сборка не удалась!
    cd ..\..
    pause
    exit /b 1
)

echo.
echo [4/5] Копирование DLL в natives...
if not exist "..\..\src\main\resources\assets\ravex\natives" mkdir "..\..\src\main\resources\assets\ravex\natives"
copy /Y "Release\*.dll" "..\..\src\main\resources\assets\ravex\natives\" 2>nul
copy /Y "*.dll" "..\..\src\main\resources\assets\ravex\natives\" 2>nul

cd ..\..

echo.
echo [5/5] Сборка Java-мода...
call gradlew.bat build

if %ERRORLEVEL% NEQ 0 (
    echo [ОШИБКА] Сборка Java не удалась!
    pause
    exit /b 1
)

echo.
echo ============================================
echo  СБОРКА ЗАВЕРШЕНА УСПЕШНО!
echo ============================================
echo.
echo Готовый JAR: build\libs\
echo Нативные DLL: src\main\resources\assets\ravex\natives\
echo.

pause
