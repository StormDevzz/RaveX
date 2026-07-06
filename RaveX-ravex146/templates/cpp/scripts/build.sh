#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATES_DIR="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$TEMPLATES_DIR/build"

echo "════════════════════════════════════════════"
echo " RaveX C++ Addon — Сборка под Linux"
echo "════════════════════════════════════════════"
echo ""

# Выбор примера (по умолчанию 02_features)
EXAMPLE="${1:-02_features}"

if [ ! -f "$TEMPLATES_DIR/$EXAMPLE/CMakeLists.txt" ]; then
    echo "[ОШИБКА] Пример '$EXAMPLE' не найден."
    echo ""
    echo "Доступные примеры:"
    for d in "$TEMPLATES_DIR"/??_*/; do
        echo "  $(basename "$d")"
    done
    echo ""
    echo "Использование: ./build.sh [имя_примера] [--install]"
    echo "  Пример: ./build.sh 01_minimal"
    echo "          ./build.sh 03_overlay --install"
    exit 1
fi

# Определяем компилятор
if command -v g++ &>/dev/null; then
    COMPILER="GCC $(g++ --version | head -1)"
    GENERATOR="Unix Makefiles"
elif command -v clang++ &>/dev/null; then
    COMPILER="Clang $(clang++ --version | head -1)"
    GENERATOR="Unix Makefiles"
else
    echo "[ОШИБКА] Не найден C++ компилятор."
    exit 1
fi

echo "Компилятор: $COMPILER"
echo "Сборка: $EXAMPLE"
echo ""

# Конфигурация CMake
cmake -S "$TEMPLATES_DIR/$EXAMPLE" -B "$BUILD_DIR/$EXAMPLE" \
    -DCMAKE_BUILD_TYPE=Release \
    -G "$GENERATOR"

# Компиляция
cmake --build "$BUILD_DIR/$EXAMPLE" -j "$(nproc)"

echo ""
echo "[OK] Сборка завершена!"
echo "[INFO] Результат: $BUILD_DIR/$EXAMPLE/"

# Установка
if [ "${2:-}" = "--install" ]; then
    INSTALL_DIR="${HOME}/.minecraft/ravex/addons/native"
    mkdir -p "$INSTALL_DIR"
    cp "$BUILD_DIR/$EXAMPLE"/*.so "$INSTALL_DIR/" 2>/dev/null || true
    echo "[INFO] Установлено в $INSTALL_DIR"
    echo "[OK] Установка завершена."
fi
