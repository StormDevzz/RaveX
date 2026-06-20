#!/bin/bash
# RaveX Java Addon compilation script example
# Run this script to package the addon into a JAR file.

# 1. Compile the sources using the RaveX client jar in the classpath
# (Adjust target paths as necessary for your local environment)
echo "Compiling Java addon sources..."
mkdir -p build
javac -cp "../../build/libs/ravex-1.4.jar" -d build AnotherAddon.java AnotherModule.java

# 2. Package into jar
echo "Packaging addon into jar..."
jar cfm AnotherAddon.jar MANIFEST.MF -C build .

# 3. Copy to addons directory
echo "Copying to RaveX addons directory..."
mkdir -p "$HOME/.minecraft/ravex/addons"
cp AnotherAddon.jar "$HOME/.minecraft/ravex/addons/"

echo "Addon successfully built and copied to ravex/addons!"
