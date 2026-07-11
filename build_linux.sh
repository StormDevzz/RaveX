#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build/native"
NATIVES_DIR="$SCRIPT_DIR/src/main/resources/assets/ravex/natives"

echo "============================================"
echo " RaveX - �������� ��������� ��������� ��� Linux"
echo "============================================"
echo ""

# [1/6] ��������� CMake
if ! command -v cmake &>/dev/null; then
    echo "[ERROR] CMake �� ������! ������������ cmake 3.16+:"
    echo "  sudo pacman -S cmake        # Arch"
    echo "  sudo apt install cmake      # Debian/Ubuntu"
    echo "  sudo dnf install cmake      # Fedora"
    exit 1
fi
echo "[INFO] CMake $(cmake --version | head -1 | awk '{print $3}'): OK"

# [2/6] ��������� ������������ (g++ ��� clang++)
CXX_COMPILER=""
if command -v g++ &>/dev/null; then
    CXX_COMPILER="g++"
elif command -v clang++ &>/dev/null; then
    CXX_COMPILER="clang++"
else
    echo "[ERROR] ������������ C++ �� ������! ������������ g++ ��� clang++."
    exit 1
fi
echo "[INFO] ������������: $CXX_COMPILER ($($CXX_COMPILER --version | head -1))"

# [3/6] ��������� JAVA_HOME
if [ -z "${JAVA_HOME:-}" ]; then
    # �������� ������������ ������
    if command -v java &>/dev/null; then
        JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$(which java)")")")
        export JAVA_HOME
    else
        echo "[ERROR] JAVA_HOME �� ���������� � java �� ������ � PATH."
        exit 1
    fi
fi
echo "[INFO] JAVA_HOME: $JAVA_HOME"
echo "[INFO] Java: $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"

# [4/6] ��������� C++ ������
C_BUILD_DIR="$SCRIPT_DIR/build/native-c"
echo ""
echo "[1/6] �������� C++ ������..."
mkdir -p "$BUILD_DIR" "$C_BUILD_DIR"

C_COMPILER="gcc"
[ "$CXX_COMPILER" = "clang++" ] && C_COMPILER="clang"

echo "[2/6] ������������ C++ CMake..."
cmake -S "$SCRIPT_DIR/src/main/cpp" -B "$BUILD_DIR" \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX="$NATIVES_DIR" \
    -DCMAKE_C_COMPILER="${CC:-$C_COMPILER}" \
    -DCMAKE_CXX_COMPILER="${CXX:-$CXX_COMPILER}"

echo "[3/6] ������ C++ ���������..."
cmake --build "$BUILD_DIR" --config Release -j"$(nproc)" || echo "[WARN] C++ build failed (optional)"

# [4/6] ��������� C ������
echo "[4/6] ������������ C CMake..."
cmake -S "$SCRIPT_DIR/src/main/c" -B "$C_BUILD_DIR" \
    -DCMAKE_BUILD_TYPE=Release

echo "[5/6] ������ C ���������..."
cmake --build "$C_BUILD_DIR" --config Release -j"$(nproc)"

# ����������� .so C++ � ��������� ������
echo "[INFO] ����������� .so � $NATIVES_DIR ..."
mkdir -p "$NATIVES_DIR"
find "$BUILD_DIR" -name "*.so" -exec cp -v {} "$NATIVES_DIR/" \; 2>/dev/null || true

echo ""
echo "�������� .so � $NATIVES_DIR:"
ls -1 "$NATIVES_DIR"/*.so 2>/dev/null | wc -l || true
echo ""

# [6/6] ������ Java-�����
echo "[6/6] ������ Java-����..."
cd "$SCRIPT_DIR"
./gradlew build

echo ""
echo "============================================"
echo "  ������ ��������� ���������!"
echo "============================================"
echo ""
echo "������� JAR: build/libs/"
echo "�������� .so: $NATIVES_DIR/"
echo ""
