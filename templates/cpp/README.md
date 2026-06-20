# RaveX C++ Native Addon Build & Installation Guide

This directory contains a template C++ native addon for RaveX, showing how to register modules and use client APIs from C++.

Addons are designed to work **only in conjunction with RaveX**. Without the RaveX client installed, they will not function.

---

## 📂 Installation Directory

The compiled shared library (`.so` on Linux, `.dll` on Windows) must be placed in the `ravex/addons/native/` folder inside your Minecraft instance (profile) directory.

| Launcher / OS | Addons Folder Path |
| :--- | :--- |
| **Standard Launcher** | `<Minecraft-game-directory>/ravex/addons/native/` |
| **Prism Launcher (Flatpak on Linux)** | `~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/<Instance_Name>/minecraft/ravex/addons/native/` |

---

## 🛠️ Build Instructions

### Requirements
* A C++ compiler supporting C++17/20 (GCC, Clang, or MSVC).
* CMake version 3.10 or higher installed.

### Build Steps

1. **Create a build directory and navigate to it**:
   ```bash
   mkdir -p build
   cd build
   ```

2. **Generate the build files using CMake**:
   ```bash
   cmake ..
   ```

3. **Compile the library** (using the Release configuration for optimization):
   ```bash
   cmake --build . --config Release
   ```
   Upon successful compilation, the built library will be generated inside the `build/` directory:
   * `AnotherAddon.so` — on Linux.
   * `AnotherAddon.dll` — on Windows.

4. **Copy the library** into the `ravex/addons/native/` folder of your Minecraft instance.

> [!TIP]
> All build and copy steps are fully automated in the [build.sh](file:///home/nprevenant/RaveX/templates/cpp/build.sh) script. You can run it locally to build the native library.
