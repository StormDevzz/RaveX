#!/bin/bash
echo "Setting up dependencies for RaveX Launcher..."

# Create directories
mkdir -p imgui/backends
mkdir -p gl3w

echo "Downloading ImGui..."
wget -O imgui.zip https://github.com/ocornut/imgui/archive/refs/tags/v1.90.6.zip
if [ $? -ne 0 ]; then
    echo "Failed to download ImGui"
    exit 1
fi

echo "Extracting ImGui..."
unzip -q imgui.zip
mv imgui-1.90.6/*.h imgui/
mv imgui-1.90.6/*.cpp imgui/
mv imgui-1.90.6/backends/imgui_impl_sdl2.cpp imgui/backends/
mv imgui-1.90.6/backends/imgui_impl_sdl2.h imgui/backends/
mv imgui-1.90.6/backends/imgui_impl_opengl3.cpp imgui/backends/
mv imgui-1.90.6/backends/imgui_impl_opengl3.h imgui/backends/
rm -rf imgui-1.90.6
rm imgui.zip

echo "Downloading gl3w..."
wget -O gl3w.zip https://github.com/skasug/gl3w/archive/refs/tags/master.zip
if [ $? -ne 0 ]; then
    echo "Failed to download gl3w"
    exit 1
fi

echo "Extracting gl3w..."
unzip -q gl3w.zip
mv gl3w-master/gl3w.h gl3w/
mv gl3w-master/gl3w.c gl3w/
mv gl3w-master/GL/gl3w.h gl3w/
rm -rf gl3w-master
rm gl3w.zip

echo "Dependencies setup complete!"
