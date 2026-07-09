@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ════════════════════════════════════════════
echo  RaveX Java Addon — Сборка под Windows
echo ════════════════════════════════════════════
echo.

set "SCRIPT_DIR=%~dp0"
set "TEMPLATES_DIR=%SCRIPT_DIR%.."
set "SRC_DIR=%TEMPLATES_DIR%\src"
set "BUILD_DIR=%TEMPLATES_DIR%\build"
set "OUTPUT_JAR=%BUILD_DIR%\MainAddon.jar"

REM Ищем RaveX JAR
set "RAVEX_JAR=%TEMPLATES_DIR%\..\..\build\libs\RaveX.jar"
if not exist "!RAVEX_JAR!" (
    echo [INFO] RaveX JAR не найден, собираем через Gradle...
    pushd "%TEMPLATES_DIR%\..\.."
    call gradlew.bat build 2>&1
    popd
)

REM Если JAR всё ещё не найден — ищем в build/libs
if not exist "!RAVEX_JAR!" (
    for %%j in ("%TEMPLATES_DIR%\..\..\build\libs\*.jar") do (
        set "RAVEX_JAR=%%j"
    )
)

if not exist "!RAVEX_JAR!" (
    echo [ОШИБКА] Не найден RaveX JAR. Собери проект сначала.
    pause
    exit /b 1
)

echo RaveX JAR: !RAVEX_JAR!

REM Создаём build директорию
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

REM Компиляция
echo Компиляция Java...
dir /s /B "%SRC_DIR%\*.java" > "%BUILD_DIR%\sources.txt"
javac -cp "!RAVEX_JAR!" -d "%BUILD_DIR%\classes" @"%BUILD_DIR%\sources.txt"
if %ERRORLEVEL% neq 0 (
    echo [ОШИБКА] Компиляция не удалась.
    pause
    exit /b 1
)

REM Копируем MANIFEST и ресурсы
copy "%SRC_DIR%\META-INF\MANIFEST.MF" "%BUILD_DIR%\classes\META-INF\MANIFEST.MF" >nul

REM Упаковка в JAR
cd "%BUILD_DIR%\classes"
jar cfm "%OUTPUT_JAR%" META-INF\MANIFEST.MF ravex\*.class 2>nul
jar uf "%OUTPUT_JAR%" META-INF\MANIFEST.MF META-INF\ 2>nul
cd "%TEMPLATES_DIR%"

echo.
echo [OK] Сборка завершена!
echo [INFO] JAR: %OUTPUT_JAR%

REM Установка
if "%1"=="--install" (
    set "INSTALL_DIR=%USERPROFILE%\.minecraft\ravex\addons"
    if not exist "!INSTALL_DIR!" mkdir "!INSTALL_DIR!"
    copy /Y "%OUTPUT_JAR%" "!INSTALL_DIR!\" >nul
    echo [INFO] Установлено: %OUTPUT_JAR% -^> !INSTALL_DIR!
    echo [OK] Установка завершена.
)

echo.
pause
