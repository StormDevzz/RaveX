#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATES_DIR="$(dirname "$SCRIPT_DIR")"
SRC_DIR="$TEMPLATES_DIR/src"
BUILD_DIR="$TEMPLATES_DIR/build"
OUTPUT_JAR="$BUILD_DIR/MainAddon.jar"

echo "════════════════════════════════════════════"
echo " RaveX Java Addon — Сборка под Linux"
echo "════════════════════════════════════════════"
echo ""

# Find RaveX JAR
RAVEX_JAR="$TEMPLATES_DIR/../../build/libs/RaveX.jar"
if [ ! -f "$RAVEX_JAR" ]; then
    echo "[INFO] RaveX JAR not found, building via Gradle..."
    pushd "$TEMPLATES_DIR/../.." >/dev/null
    ./gradlew build 2>&1
    popd >/dev/null
fi

# Fallback: find any jar in build/libs
if [ ! -f "$RAVEX_JAR" ]; then
    RAVEX_JAR=$(ls "$TEMPLATES_DIR/../../build/libs/"*.jar 2>/dev/null | head -1 || true)
fi

if [ -z "$RAVEX_JAR" ] || [ ! -f "$RAVEX_JAR" ]; then
    echo "[ERROR] RaveX JAR not found. Build the project first."
    exit 1
fi

echo "RaveX JAR: $RAVEX_JAR"

# Create build directory
mkdir -p "$BUILD_DIR/classes"

# Compile
echo "Compiling Java sources..."
find "$SRC_DIR" -name "*.java" > "$BUILD_DIR/sources.txt"
javac -cp "$RAVEX_JAR" -d "$BUILD_DIR/classes" @"$BUILD_DIR/sources.txt"

# Copy manifest
cp "$SRC_DIR/META-INF/MANIFEST.MF" "$BUILD_DIR/classes/META-INF/MANIFEST.MF"

# Package JAR
cd "$BUILD_DIR/classes"
jar cfm "$OUTPUT_JAR" META-INF/MANIFEST.MF ravex/*.class 2>/dev/null
jar uf "$OUTPUT_JAR" META-INF/MANIFEST.MF META-INF/ 2>/dev/null
cd "$TEMPLATES_DIR"

echo ""
echo "[OK] Build complete!"
echo "[INFO] JAR: $OUTPUT_JAR"

# Install
if [ "${1:-}" = "--install" ]; then
    INSTALL_DIR="${HOME}/.minecraft/ravex/addons"
    mkdir -p "$INSTALL_DIR"
    cp "$OUTPUT_JAR" "$INSTALL_DIR/"
    echo "[INFO] Installed: $OUTPUT_JAR -> $INSTALL_DIR/"
    echo "[OK] Install complete."
fi
