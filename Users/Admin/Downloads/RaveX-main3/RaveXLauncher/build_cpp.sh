#!/bin/bash
echo "Building RaveX Launcher (C++)..."

# Create build directory
mkdir -p build
cd build

# Generate project files with CMake
cmake ..
if [ $? -ne 0 ]; then
    echo "CMake generation failed"
    cd ..
    exit 1
fi

# Build
make -j$(nproc)
if [ $? -ne 0 ]; then
    echo "Build failed"
    cd ..
    exit 1
fi

cd ..
echo "Build complete! Executable is in build directory."
