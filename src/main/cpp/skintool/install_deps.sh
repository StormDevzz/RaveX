#!/bin/sh
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"

detect_distro() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        echo "$ID"
    elif command -v lsb_release >/dev/null 2>&1; then
        lsb_release -is | tr '[:upper:]' '[:lower:]'
    else
        echo "unknown"
    fi
}

install_arch() {
    sudo pacman -S --needed base-devel cmake qt6-base glew
}

install_debian() {
    sudo apt update
    sudo apt install -y build-essential cmake qt6-base-dev libglew-dev
}

install_fedora() {
    sudo dnf install -y @development-tools cmake qt6-qtbase-devel glew-devel
}

DISTRO=$(detect_distro)
case "$DISTRO" in
    arch|manjaro|endeavouros)    install_arch ;;
    debian|ubuntu|mint|kali)     install_debian ;;
    fedora|rhel|centos)          install_fedora ;;
    *)
        echo "Unknown distro: $DISTRO"
        echo "Install cmake, C++23 compiler, Qt6, GLEW, OpenGL manually."
        exit 1
        ;;
esac

echo "Dependencies installed. Building..."
cmake -S "$DIR" -B "$DIR/build"
cmake --build "$DIR/build" -j"$(nproc)"
echo "Done. Binary: $DIR/build/skintool"
