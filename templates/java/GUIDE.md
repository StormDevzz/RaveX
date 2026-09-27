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
import ravex.addon.core.AddonContext;
import ravex.addon.core.AddonInfo;

public class MainAddon implements Addon {
    private AddonContext context;

    @Override
    public void onLoad(AddonContext context) {
        this.context = context;
        context.registerModule(new DemoModule(this));
    }

    @Override
    public void onUnload() {
    }

    @Override
    public AddonInfo getInfo() {
        return new AddonInfo("MainAddon", "Description",
            "1.0.0", "Author", "ravex.addon.template.MainAddon");
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
package ravex.addon.template;

import ravex.addon.Addon;
import ravex.addon.module.AddonModule;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.PlayerUtility;

public class DemoModule extends AddonModule {
    @Parameter(name = "FeatureEnabled")
    public boolean featureEnabled = true;

    @Parameter(name = "Speed", min = 0.1, max = 5.0, step = 0.1)
    public double speed = 1.5;

    @Parameter(name = "Mode", modes = {"Basic", "Advanced"})
    public String mode = "Basic";

    public DemoModule(Addon parent) {
        super("DemoModule", "Custom", parent);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onTick() {
        if (!featureEnabled) {
            return;
        }
        if (PlayerUtility.getPlayer() == null) {
            return;
        }
    }
}
```

### Parameters

Settings are `@Parameter` annotations on primitive fields. `Module` creates the wrappers via `ParameterFactory`.

### Platform branches in onTick

```java
@Override
public void onTick() {
    if (MainAddon.isWindows()) {
        tickWindows();
    } else {
        tickLinux();
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
jar cfm ../MainAddon.jar META-INF/MANIFEST.MF .
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
| `Addon` | `onLoad`, `onUnload`, `getInfo` | Main addon class |
| `AddonModule` | `onEnable`, `onDisable`, `onTick` | Module (functionality) |
| `AddonListener` | `onEvent` | Event listener |

### Management

| Class | Methods | Purpose |
|-------|---------|---------|
| `AddonContext` | `getLogger`, `getInfo`, `registerModule` | Context |
| `AddonLoader` | `loadAddon` | JAR loading via manifest |
| `AddonInfo` | (constructor) | Metadata |

### Module parameters

Settings are `@Parameter` on primitives (`boolean`, `double`, `int`, `String`). Wrappers in `ravex.parameter` are created automatically.

---

## 10. FAQ

### ❓ My addon doesn't load

1. Check `MANIFEST.MF` — `Addon-Main-Class` must match the real class
2. Check that the class implements `ravex.addon.Addon` with `getInfo`
3. Check Minecraft console (`.minecraft/logs/latest.log`)
4. Check signature: `<name>.jar.ravex-sig` must sit next to the JAR

### ❓ Need more performance offload heavy computation to C++

See `templates/cpp/02_features/` for the JNI bridge setup.

---

## Next Steps

1. **Build the example** — `cd scripts && build.bat` or `./build.sh`
2. **Study `MainAddon.java`** — understand the lifecycle
3. **Add your own modules** — follow `DemoModule.java`
4. **Integrate with C++** — load native libs via `System.load()`
5. **See C++ examples** — in `templates/cpp/` for advanced features
