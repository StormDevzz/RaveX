@echo off
echo Setting up dependencies for RaveX Launcher...

REM Create directories
if not exist imgui mkdir imgui
if not exist imgui\backends mkdir imgui\backends
if not exist gl3w mkdir gl3w

echo Downloading ImGui...
curl -L -o imgui.zip https://github.com/ocornut/imgui/archive/refs/tags/v1.90.6.zip
if %errorlevel% neq 0 (
    echo Failed to download ImGui
    pause
    exit /b 1
)

echo Extracting ImGui...
powershell -Command "Expand-Archive -Path imgui.zip -DestinationPath . -Force"
move imgui-1.90.6\*.h imgui\
move imgui-1.90.6\*.cpp imgui\
move imgui-1.90.6\backends\imgui_impl_sdl2.cpp imgui\backends\
move imgui-1.90.6\backends\imgui_impl_sdl2.h imgui\backends\
move imgui-1.90.6\backends\imgui_impl_opengl3.cpp imgui\backends\
move imgui-1.90.6\backends\imgui_impl_opengl3.h imgui\backends\
rmdir /s /q imgui-1.90.6
del imgui.zip

echo Downloading gl3w...
curl -L -o gl3w.zip https://github.com/skasug/gl3w/archive/refs/tags/master.zip
if %errorlevel% neq 0 (
    echo Failed to download gl3w
    pause
    exit /b 1
)

echo Extracting gl3w...
powershell -Command "Expand-Archive -Path gl3w.zip -DestinationPath . -Force"
move gl3w-master\gl3w.h gl3w\
move gl3w-master\gl3w.c gl3w\
move gl3w-master\GL\gl3w.h gl3w\
rmdir /s /q gl3w-master
del gl3w.zip

echo Dependencies setup complete!
pause
