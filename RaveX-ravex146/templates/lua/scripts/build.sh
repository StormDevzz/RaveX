#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATES_DIR="$(dirname "$SCRIPT_DIR")"

echo "============================================"
echo " RaveX Lua Addon — Deploy"
echo "============================================"
echo ""

EXAMPLE="${1:-01_minimal}"

if [ ! -f "$TEMPLATES_DIR/$EXAMPLE/main.lua" ]; then
    echo "[ERROR] Example '$EXAMPLE' not found."
    echo ""
    echo "Available examples:"
    for d in "$TEMPLATES_DIR"/??_*/; do
        echo "  $(basename "$d")"
    done
    echo ""
    echo "Usage: ./build.sh [example_name]"
    echo "  Example: ./build.sh 01_minimal"
    exit 1
fi

LUA_DIR="${HOME}/.minecraft/ravex/addons/lua/${EXAMPLE}"
mkdir -p "$LUA_DIR"

cp "$TEMPLATES_DIR/$EXAMPLE/main.lua" "$LUA_DIR/"

echo "[OK] Deployed: $EXAMPLE"
echo "[INFO] $TEMPLATES_DIR/$EXAMPLE/main.lua -> $LUA_DIR/"
