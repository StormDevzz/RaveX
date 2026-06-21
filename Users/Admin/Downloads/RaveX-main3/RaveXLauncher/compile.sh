#!/bin/bash
echo "Compiling RaveX Launcher..."

# Create build directory
mkdir -p build/classes

# Compile Java files
javac -d build/classes -source 1.8 -target 1.8 src/main/java/ravex/launcher/RaveXLauncher.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Creating JAR file..."

# Create manifest file
echo "Main-Class: ravex.launcher.RaveXLauncher" > build/manifest.txt

# Create JAR
cd build/classes
jar cvfm ../RaveXLauncher.jar ../manifest.txt ravex/launcher/*.class
cd ../..

echo "Build complete! RaveXLauncher.jar created in build directory."
