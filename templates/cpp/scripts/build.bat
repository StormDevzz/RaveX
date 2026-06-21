@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ════════════════════════════════════════════
echo  RaveX C++ Addon — Сборка под Windows
echo ════════════════════════════════════════════
echo.

REM Определяем корень шаблонов
set "TEMPLATES_DIR=%~dp0.."
set "BUILD_DIR=%TEMPLATES_DIR%\build"

REM Выбор примера (по умолчанию 02_features)
if "%1"=="" (
    set "EXAMPLE=02_features"
) else (
    set "EXAMPLE=%1"
)

if not exist "%TEMPLATES_DIR%\%EXAMPLE%\CMakeLists.txt" (
    echo [ОШИБКА] Пример "%EXAMPLE%" не найден.
    echo.
    echo Доступные примеры:
    for /d %%i in ("%TEMPLATES_DIR%\??_*") do echo   %%~nxi
    echo.
    echo Использование: build.bat [имя_примера] [--install]
    echo   Пример: build.bat 01_minimal
    echo          build.bat 03_overlay --install
    pause
    exit /b 1
)

REM Проверка CMake
where cmake >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ОШИБКА] CMake не найден. Установи CMake 3.16+ и добавь в PATH.
    pause
    exit /b 1
)

REM Выбор компилятора
where g++ >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo Компилятор: MinGW-W64 (GCC)
    set "CMAKE_GEN=-G "MinGW Makefiles""
) else (
    where cl >nul 2>&1
    if !ERRORLEVEL! equ 0 (
        echo Компилятор: MSVC
        set "CMAKE_GEN="
    ) else (
        echo [ОШИБКА] Не найден C++ компилятор.
        pause
        exit /b 1
    )
)

echo Сборка: %EXAMPLE%
echo.

REM Конфигурация CMake
cmake -S "%TEMPLATES_DIR%\%EXAMPLE%" -B "%BUILD_DIR%\%EXAMPLE%" -DCMAKE_BUILD_TYPE=Release %CMAKE_GEN%
if %ERRORLEVEL% neq 0 (
    echo [ОШИБКА] Конфигурация CMake не удалась.
    pause
    exit /b 1
)

REM Компиляция
cmake --build "%BUILD_DIR%\%EXAMPLE%" --config Release --parallel
if %ERRORLEVEL% neq 0 (
    echo [ОШИБКА] Сборка не удалась.
    pause
    exit /b 1
)

echo.
echo [OK] Сборка завершена!
echo [INFO] Результат: %BUILD_DIR%\%EXAMPLE%\

REM Установка
if "%2"=="--install" (
    set "INSTALL_DIR=%USERPROFILE%\.minecraft\ravex\addons\native"
    if not exist "!INSTALL_DIR!" mkdir "!INSTALL_DIR!"
    for %%f in ("%BUILD_DIR%\%EXAMPLE%\*.dll") do (
        copy /Y "%%f" "!INSTALL_DIR!\" >nul
        echo [INFO] Установлено: %%f -^> !INSTALL_DIR!
    )
    echo [OK] Установка завершена.
)

echo.
pause
