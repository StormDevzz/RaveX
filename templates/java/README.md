# RaveX Java Addon Build & Installation Guide

This directory contains a template Java addon for RaveX that registers a `Custom` category and an `Another` module.

Addons are designed to work **only in conjunction with RaveX**. Without the RaveX client installed, they will not function.

---

## Installation Directory

The compiled JAR file of the addon must be placed in the `ravex/addons/` folder inside your Minecraft instance (profile) directory.

| Launcher / OS | Addons Folder Path |
| :--- | :--- |
| **Standard Launcher** | `<Minecraft-game-directory>/ravex/addons/` |
| **Prism Launcher (Flatpak on Linux)** | `~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/<Instance_Name>/minecraft/ravex/addons/` |

> [!WARNING]
> Do not place the addon JAR file into the launcher's `mods/` directory. Doing so will cause a Fabric Loader warning (Found 1 non-fabric mod) and the addon will not be loaded by RaveX.

---

## Build Instructions

### Requirements
* JDK 21 or newer installed.
* Compiled RaveX client library (`ravex-1.4.jar`).

### Build Steps

1. **Build the main RaveX client** in the project root:
   ```bash
   ./gradlew build
   ```
   The client library will be generated at `build/libs/ravex-1.4.jar`.

2. **Compile the addon source files**:
   ```bash
   mkdir -p build
   javac -cp "../../build/libs/ravex-1.4.jar" -d build AnotherAddon.java AnotherModule.java
   ```

3. **Verify the manifest file** (`MANIFEST.MF`):
   ```manifest
   Manifest-Version: 1.0
   Addon-Name: AnotherAddon
   Addon-Version: 1.0.0
   Addon-Author: RaveXDeveloper
   Addon-Main-Class: ravex.addon.template.AnotherAddon
   ```
   *(Ensure there is a trailing empty line at the end of the manifest file).*

4. **Package the compiled classes into a JAR**:
   ```bash
   jar cfm AnotherAddon.jar MANIFEST.MF -C build .
   ```

5. **Copy `AnotherAddon.jar`** into the `ravex/addons/` folder of your Minecraft instance.

> [!TIP]
> All build and copy steps are fully automated in the [build.sh](file:///home/nprevenant/RaveX/templates/java/build.sh) script. You can run it locally to package the addon.
