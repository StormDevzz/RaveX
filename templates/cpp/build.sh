#!/bin/bash
# RaveX Native C++ Addon compilation script example

echo "Configuring CMake build..."
mkdir -p build
cd build
cmake ..

echo "Compiling native addon library..."
cmake --build . --config Release

echo "Copying to RaveX addons directory..."
# Copies to the default game directory ravex/addons/native
mkdir -p "$HOME/.minecraft/ravex/addons/native"
# Copy .so on Linux
cp *.so "$HOME/.minecraft/ravex/addons/native/" 2>/dev/null || true
# Copy .dll on Windows
cp *.dll "$HOME/.minecraft/ravex/addons/native/" 2>/dev/null || true

echo "Native addon built and copied to ravex/addons/native!"
