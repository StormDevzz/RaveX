@echo off
echo Compiling RaveX Launcher...

REM Create build directory
if not exist build mkdir build
if not exist build\classes mkdir build\classes

REM Compile Java files
javac -d build\classes -source 1.8 -target 1.8 src\main\java\ravex\launcher\RaveXLauncher.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Creating JAR file...

REM Create manifest file
echo Main-Class: ravex.launcher.RaveXLauncher > build\manifest.txt

REM Create JAR
cd build\classes
"D:\Program Files\Java\jdk-25\bin\jar.exe" cvfm ..\RaveXLauncher.jar ..\manifest.txt ravex\launcher\*.class
cd ..\..

echo Build complete! RaveXLauncher.jar created in build directory.
pause
