# RaveX Java Addons — Complete Guide

> Version: 1.0 | Platforms: Windows 10/11, Linux | Language: Java 17+

---

## Table of Contents

1. [What is a Java Addon?](#1-what-is-a-java-addon)
2. [Project Structure](#2-project-structure)
3. [MainAddon.java — Main Class](#3-mainaddonjava--main-class)
4. [DemoModule.java — Module](#4-demomodulejava--module)
5. [MANIFEST.MF — JAR Manifest](#5-manifestmf--jar-manifest)
6. [Cross-Platform Programming](#6-cross-platform-programming)
7. [C++ Integration via JNI](#7-c-integration-via-jni)
8. [Building & Installing](#8-building--installing)
9. [RaveX Addon API](#9-ravex-addon-api)
10. [FAQ](#10-faq)

---

## 1. What is a Java Addon?

A **Java addon** is a JAR file that RaveX loads at runtime using a custom ClassLoader.

### Java vs C++ Addons

| Aspect | Java Addon | C++ Native Addon |
|--------|-----------|-------------------|
| Complexity | Low | High |
| Cross-platform | Yes Write once, run anywhere | Two implementations |
| RaveX API access | Yes Full | Yes Via JNI |
| Performance | Medium | High |
| System calls | No (Sandbox) | Yes Win32 / X11 |
| Overlay | No | Yes Win32 / X11 |
| Binary size | Yes Small (JAR) | Medium (DLL/SO) |
| Hot-reload | No | No |

**Choose Java when:**
- Simple logic (commands, GUI, event listeners)
- No system calls needed
- Quick prototyping
- Integrating with existing Java libraries

**Choose C++ when:**
- Maximum performance
- Overlay windows
- Process memory interaction
- Hardware acceleration

---

## 2. Project Structure

```
templates/java/
├── src/
│   ├── ravex/addon/template/
│   │   ├── MainAddon.java       # Main addon class
│   │   └── DemoModule.java      # Module example
│   └── META-INF/
│       └── MANIFEST.MF          # JAR manifest
├── scripts/
│   ├── build.bat                # Windows build
│   └── build.sh                 # Linux build
├── GUIDE.md                     # This guide (English)
├── GUIDE_RU.md                  # Russian version
└── README.md                    # Quick start
```

### File roles

| File | Purpose |
|------|---------|
| `MainAddon.java` | Entry point: init, native loading, module registration |
| `DemoModule.java` | Module with parameters and platform branches |
| `MANIFEST.MF` | JAR manifest with main class specification |
| `build.bat` / `build.sh` | One-click build & install |

---

## 3. MainAddon.java — Main Class

### Minimum required

```java
package ravex.addon.template;

import ravex.addon.Addon;
import ravex.addon.AddonContext;
import ravex.addon.AddonInfo;

public class MainAddon implements Addon {

    @Override
    public void onLoad(AddonContext context) {
        // Initialization
    }

    @Override
    public void onUnload() {
        // Cleanup
    }

    @Override
    public AddonInfo getAddonInfo() {
        return new AddonInfo("MainAddon", "Description",
            "1.4.1", "Author", "ravex.addon.template.MainAddon");
    }
}
```

### Platform detection

```java
public static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
}

public static String getPathSep() {
    return isWindows() ? "\\" : "/";
}
```

### Loading native libraries

```java
System.load(nativeDir + getPathSep()
    + System.mapLibraryName("MyAddon"));
// Windows → "MyAddon.dll"
// Linux   → "libMyAddon.so"
```

---

## 4. DemoModule.java — Module

### Basic structure

```java
public class DemoModule extends AddonModule {

    public DemoModule() {
        super("DemoModule", "Demo", AddonModuleInfo.Category.CUSTOM);
    }

    @Override
    public void onEnable() { }
    @Override
    public void onDisable() { }
    @Override
    public void onTick() { }
}
```

### Parameters (auto-displayed in RaveX GUI)

```java
private final BooleanParameter enabled = new BooleanParameter("enabled", true);
private final NumberParameter  speed   = new NumberParameter("speed", 1.0, 0.1, 5.0);
```

### Platform branches in onTick

```java
@Override
public void onTick() {
    if (MainAddon.isWindows()) {
        tickWindows();   // Windows-specific logic
    } else {
        tickLinux();     // Linux-specific logic
    }
}
```

---

## 5. MANIFEST.MF — JAR Manifest

```mf
Addon-Name: MainAddon
Addon-Version: 1.0
Addon-Author: RaveX Team
Addon-Main-Class: ravex.addon.template.MainAddon
```

**Important:**
- `Addon-Main-Class` — fully qualified class name implementing `Addon`
- File must be at `META-INF/MANIFEST.MF` inside the JAR

---

## 6. Cross-Platform Programming

### OS detection

```java
String os = System.getProperty("os.name").toLowerCase();
if (os.contains("win")) {
    // Windows
} else if (os.contains("nix") || os.contains("nux")) {
    // Linux
}
```

### Paths

| Operation | Windows | Linux |
|-----------|---------|-------|
| Separator | `\` | `/` |
| Home | `C:\Users\name` | `/home/name` |
| Minecraft | `%USERPROFILE%\.minecraft` | `~/.minecraft` |
| Addons | `%USERPROFILE%\.minecraft\ravex\addons\` | `~/.minecraft/ravex/addons/` |
| Native | `%USERPROFILE%\.minecraft\ravex\addons\native\` | `~/.minecraft/ravex/addons/native/` |

### Native library loading

| Platform | Filename | Java name |
|----------|----------|-----------|
| Windows | `MyAddon.dll` | `"MyAddon"` → maps to `"MyAddon.dll"` |
| Linux | `libMyAddon.so` | `"MyAddon"` → maps to `"libMyAddon.so"` |

---

## 7. C++ Integration via JNI

The real power of RaveX comes from combining Java + C++.

### Step 1: C++ implements native methods

In `02_features/JniBridge.cpp`:

```cpp
extern "C" JNIEXPORT jint JNICALL
Java_ravex_addon_jni_JniBridge_nativeAdd(JNIEnv*, jclass, jint a, jint b) {
    return a + b;
}
```

### Step 2: Java declares native methods

```java
public class JniBridge {
    static { System.loadLibrary("FeatureAddon"); }
    public static native int  nativeAdd(int a, int b);
    public static native String nativeGetPlatformInfo();
}
```

### Step 3: Call from Java

```java
int sum = JniBridge.nativeAdd(40, 2);           // 42
String info = JniBridge.nativeGetPlatformInfo();
```

### C++ calling Java (Callback)

```cpp
void fireEvent(const char* data) {
    JNIEnv* env;
    g_jvm->AttachCurrentThread((void**)&env, nullptr);
    env->CallVoidMethod(g_obj, g_callback, env->NewStringUTF(data));
}
```

---

## 8. Building & Installing

### Quick build (scripts)

```bash
# Linux
cd templates/java/scripts
chmod +x build.sh
./build.sh                    # Build
./build.sh --install          # Build + install
```

```cmd
REM Windows
cd templates\java\scripts
build.bat                     % Build
build.bat --install           % Build + install
```

### Manual build

```bash
cd templates/java
javac -cp ../../build/libs/RaveX.jar \
    -d build/classes \
    src/ravex/addon/template/*.java
cp src/META-INF/MANIFEST.MF build/classes/META-INF/
cd build/classes
jar cfm ../MainAddon.jar META-INF/MANIFEST.MF ravex/*.class
```

### Installation

Copy the JAR to the addons folder:

| Platform | Path |
|----------|------|
| Windows | `%USERPROFILE%\.minecraft\ravex\addons\` |
| Linux | `~/.minecraft/ravex/addons/` |

---

## 9. RaveX Addon API

### Core interfaces

| Interface | Methods | Purpose |
|-----------|---------|---------|
| `Addon` | `onLoad`, `onUnload`, `getAddonInfo` | Main addon class |
| `AddonModule` | `onEnable`, `onDisable`, `onTick` | Module (functionality) |
| `AddonListener` | `onEvent` | Event listener |

### Management

| Class | Methods | Purpose |
|-------|---------|---------|
| `AddonContext` | `getLogger`, `getAddonName`, `getDataDir` | Context |
| `AddonModuleManager` | `registerModule`, `unregisterModule`, `getLogger` | Module registry |
| `AddonInfo` | (constructor) | Metadata |

### Module parameters

| Class | Type | Example |
|-------|------|---------|
| `BooleanParameter` | `boolean` | `new BooleanParameter("enabled", true)` |
| `NumberParameter` | `double` | `new NumberParameter("speed", 1.0, 0.1, 5.0)` |
| `StringParameter` | `String` | `new StringParameter("mode", "default")` |
| `ColorParameter` | `int` (0xRRGGBB) | `new ColorParameter("color", 0x00FF00)` |

---

## 10. FAQ

### ❓ My addon doesn't load

1. Check `MANIFEST.MF` — is `Addon-Main-Class` correct?
2. Check Minecraft console (`.minecraft/logs/latest.log`)
3. Try running manually: `javac -cp RaveX.jar MainAddon.java`

### ❓ Native library not loading

1. Architecture mismatch: 64-bit Java needs 64-bit DLL
2. Wrong path: `%USERPROFILE%\.minecraft\ravex\addons\native\MyAddon.dll`
3. Missing dependencies: use Dependency Walker (Windows) or `ldd` (Linux)

### ❓ Need more performance offload heavy computation to C++

See `templates/cpp/02_features/` for the JNI bridge setup.

---

## Next Steps

1. **Build the example** — `cd scripts && build.bat` or `./build.sh`
2. **Study `MainAddon.java`** — understand the lifecycle
3. **Add your own modules** — follow `DemoModule.java`
4. **Integrate with C++** — load native libs via `System.load()`
5. **See C++ examples** — in `templates/cpp/` for advanced features
