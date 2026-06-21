# RaveX Native C++ Addons — Complete Guide

> Version: 1.0 | Platforms: Windows 10/11, Linux | Language: C++23

---

## Table of Contents

1. [What is a Native Addon?](#1-what-is-a-native-addon)
2. [Project Structure](#2-project-structure)
3. [01 Minimal Addon](#3-01-minimal-addon)
4. [02 Full-Featured Addon](#4-02-full-featured-addon)
5. [03 Overlay Window](#5-03-overlay-window)
6. [04 GitHub Auto-Update](#6-04-github-auto-update)
7. [Cross-Platform Programming](#7-cross-platform-programming)
8. [JNI: Java + C++](#8-jni-java--c)
9. [Building & Installing](#9-building--installing)
10. [API Reference](#10-api-reference)

---

## 1. What is a Native Addon?

A **native addon** is a dynamic library (`.dll` on Windows, `.so` on Linux) that RaveX loads at runtime via `LoadLibrary` / `dlopen`.

### Why C++ when Java exists?

| Task | Java | C++ |
|------|------|-----|
| Speed | ~100-500 ops/ms | ~1,000,000+ ops/ms |
| Win32 API access | No | Yes `CreateWindowEx`, `DirectX`, `WinHTTP` |
| X11/Linux API access | No | Yes `Xlib`, `POSIX sockets`, `ioctl` |
| Memory read/write | No (Sandbox) | Yes `ReadProcessMemory`, `ptrace` |
| Overlay window | No | Yes Win32 layered / X11 override-redirect |
| HTTP requests | Yes `java.net.URL` | Yes WinHTTP / POSIX sockets |
| Binary size | ~2 KB JAR | ~50-500 KB DLL/SO |
| Development ease | Yes High | Medium |

### When to use C++?

- **Overlay**: FPS counter, radar, server info rendered on top of the game
- **Performance**: math, rendering, data processing
- **System calls**: process control, priorities, memory management
- **Networking**: direct HTTP requests (no Java layer)
- **Hardware acceleration**: Win32 GDI, Direct2D, X11

---

## 2. Project Structure

```
templates/cpp/
├── 01_minimal/              # 1. Minimal addon (53 lines)
│   ├── CMakeLists.txt        #   Build configuration
│   └── main.cpp              #   Source code
├── 02_features/             # 2. Full-featured addon
│   ├── CMakeLists.txt        #   + pthread/winmm, JNI
│   ├── main.cpp              #   ~200 lines with platform branches
│   ├── platform.hpp          #   ADDON_* macros (116 lines)
│   ├── JniBridge.hpp         #   JNI helpers: jstring2str, fireCallback
│   └── JniBridge.cpp         #   JNI bridge implementation
├── 03_overlay/              # 3. Overlay window
│   ├── CMakeLists.txt        #   + gdi32, dwmapi (Win) / X11 (Linux)
│   └── main.cpp              #   Win32Overlay + X11Overlay classes
├── 04_github/               # 4. GitHub auto-update
│   ├── CMakeLists.txt        #   + ravex_github_tools library
│   └── main.cpp              #   ReleaseChecker, ReleaseManager
├── scripts/
│   ├── build.bat             # Universal Windows build script
│   └── build.sh              # Universal Linux build script
├── GUIDE.md                  # This guide (English)
├── GUIDE_RU.md               # Russian version
└── README.md                 # Quick start + navigation
```

### Principle: each example is self-contained

Every directory (`01_minimal`, `02_features`, etc.) is a **complete CMake project**:

```bash
cd 02_features
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release -G "MinGW Makefiles"
cmake --build . -j
```

Or use the shared script:
```bash
cd scripts
./build.sh 02_features --install     # Linux
build.bat 02_features --install      # Windows
```

---

## 3. 01 Minimal Addon

**Goal**: understand the absolute minimum required for any native addon.

### What's needed?

1. Inherit from `ravex::addon::Addon`
2. Override 4 methods: `onLoad`, `onUnload`, `getName`, `getVersion`
3. Export two `extern "C"` functions: `createAddon` and `destroyAddon`

### Full code (`01_minimal/main.cpp`):

```cpp
#include "../../../src/main/cpp/addon/include/Addon.h"
#include "../../../src/main/cpp/addon/include/AddonContext.h"

namespace ravex { namespace addon { namespace minimal_addon {

class MinimalAddon : public Addon {
public:
    void onLoad(AddonContext* ctx) override {
        ctx->logInfo("MinimalAddon loaded!");
    }

    void onUnload() override { /* cleanup */ }
    std::string getName()    const override { return "MinimalAddon"; }
    std::string getVersion() const override { return "1.4.1"; }
};

}}}

extern "C" {
    ADDON_API ravex::addon::Addon* createAddon() {
        return new ravex::addon::minimal_addon::MinimalAddon();
    }
    ADDON_API void destroyAddon(ravex::addon::Addon* addon) {
        delete addon;
    }
}
```

### How it works

1. RaveX finds the `.dll`/`.so` in the addons folder
2. Calls `LoadLibrary` / `dlopen`
3. Looks up `createAddon` via `GetProcAddress` / `dlsym`
4. Creates the addon instance
5. Calls `onLoad(ctx)` — addon is ready
6. On unload: calls `onUnload()`, then `destroyAddon()`

### Key points

- **ADDON_API** macro (from `platform.hpp`):
  - Windows: `__declspec(dllexport)`
  - Linux: `__attribute__((visibility("default")))`
- Without this, Linux won't export symbols (due to `-fvisibility=hidden`)
- Names `createAddon` and `destroyAddon` **are mandatory**

---

## 4. 02 Full-Featured Addon

**Goal**: learn to use the RaveX API, platform branches, config, events, threads, and JNI.

### CMakeLists.txt highlights

```cmake
if(WIN32)
    target_link_libraries(FeatureAddon PRIVATE winmm psapi)
else()
    find_package(Threads REQUIRED)
    target_link_libraries(FeatureAddon PRIVATE Threads::Threads)
endif()

add_library(FeatureAddon SHARED main.cpp JniBridge.cpp)
target_include_directories(FeatureAddon PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}                       # platform.hpp
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../src/main/cpp/addon/include
)
```

### File breakdown

#### `main.cpp` — addon logic in 3 parts

**Part 1: Platform utilities**
```cpp
static std::string getAddonDataDir() {
#ifdef _WIN32
    char path[MAX_PATH];
    SHGetFolderPathA(NULL, CSIDL_LOCAL_APPDATA, NULL, 0, path);
    return std::string(path) + "\\RaveX\\FeatureAddon";
#else
    const char* home = getenv("HOME");
    return std::string(home) + "/.ravex/FeatureAddon";
#endif
}
```

**Part 2: Event listener + worker thread**
```cpp
class FeatureListener : public AddonListener {
    void onEvent(AddonEvent& event) override {
        // React to RaveX events
    }
};

class FeatureAddon : public Addon {
    std::thread worker;
    void workerThread() {
        ADDON_SET_NORMAL_PRIORITY();
        while (running) {
            // Work...
            ADDON_SLEEP(50);
        }
    }
};
```

**Part 3: JNI functions**
```cpp
extern "C" JNIEXPORT void JNICALL
Java_ravex_addon_feature_FeatureAddon_nativeLog(JNIEnv* env, jclass, jstring msg) {
    const char* str = env->GetStringUTFChars(msg, nullptr);
    std::cout << "[C++] " << str << std::endl;
    env->ReleaseStringUTFChars(msg, str);
}
```

#### `platform.hpp` — cross-platform macros

A single header that hides platform differences:

```cpp
ADDON_SLEEP(100)              // Sleep(100) on Win, usleep(100000) on Linux
ADDON_SET_HIGH_PRIORITY()     // SetPriorityClass on Win, setpriority on Linux
ADDON_TRIM_MEMORY()           // EmptyWorkingSet on Win, malloc_trim(0) on Linux
ADDON_LOAD_LIB("mylib.dll")   // LoadLibraryA on Win, dlopen on Linux
```

See `02_features/platform.hpp` for the complete list.

#### `JniBridge.hpp/cpp` — Java ↔ C++ bridge

```cpp
// In Java:
int result = JniBridge.nativeAdd(40, 2);
String info = JniBridge.nativeGetPlatformInfo();

// In C++ (JniBridge.cpp):
JNIEXPORT jint JNICALL
Java_ravex_addon_jni_JniBridge_nativeAdd(JNIEnv*, jclass, jint a, jint b) {
    return a + b;                           // 42
}
```

---

## 5. 03 Overlay Window

**Goal**: create a transparent, click-through overlay window on top of the game to show FPS, server info, etc.

### Windows: Win32 Overlay

| Aspect | Details |
|--------|---------|
| Window type | Layered Window (`WS_EX_LAYERED`) |
| Transparency | `SetLayeredWindowAttributes` + `ULW_ALPHA` |
| Click-through | `WS_EX_TRANSPARENT` |
| Always on top | `HWND_TOPMOST` |
| Rendering | GDI double-buffering via `m_memDc` |
| Acrylic/blur | `DwmEnableBlurBehindWindow` (Win10+) |

**Frame lifecycle:**
```
beginFrame()
  → Clear (black background)
  → SetBkMode(TRANSPARENT)

endFrame()
  → Call renderCallback
  → UpdateLayeredWindow() — alpha blend to screen
```

### Linux: X11 Overlay

```cpp
Display* d = XOpenDisplay(nullptr);
Window w = XCreateSimpleWindow(d, root, x, y, w, h, 0, 0, 0);

// Disable window manager control
attrs.override_redirect = True;
XChangeWindowAttributes(d, w, CWOverrideRedirect, &attrs);

// Above all windows
Atom above = XInternAtom(d, "_NET_WM_STATE_ABOVE", False);
XChangeProperty(d, w, XInternAtom(d, "_NET_WM_STATE", False),
    XA_ATOM, 32, PropModeReplace, (unsigned char*)&above, 1);

XMapWindow(d, w);
XFlush(d);
```

### Running the overlay from an addon

The overlay runs in a **separate thread**:

```cpp
void onLoad(AddonContext* ctx) override {
    running = true;
    overlayThread = new std::thread(&OverlayAddon::overlayLoop, this);
}

void overlayLoop() {
    overlay.create(0, 0, 400, 200);
    while (running) {
        overlay.beginFrame();
        // Update FPS, CPU, RAM metrics
        overlay.endFrame();
        ADDON_SLEEP(33);  // ~30 FPS
    }
    overlay.destroy();
}
```

---

## 6. 04 GitHub Auto-Update

**Goal**: automatically check for new RaveX releases on GitHub, download and install them.

Uses the `ravex_github_tools` library from `src/main/cpp/plugins/github/`.

### ReleaseChecker

```cpp
ravex::github::ReleaseChecker checker("StormDevzz", "RaveX");

auto info = checker.checkForUpdates("1.4.1");
// info.available → true/false
// info.remoteVersion → "1.5.1"
```

### ReleaseManager (full workflow)

```cpp
ravex::github::GithubConfig cfg;
cfg.owner = "StormDevzz";
cfg.repo  = "RaveX";
cfg.currentVersion = "1.4.1";

ravex::github::ReleaseManager manager(cfg);
manager.onProgress([](int64_t dl, int64_t total) {
    printf("\rDownloaded: %lld / %lld", dl, total);
});

auto info = manager.check();          // 1. Check
auto dl   = manager.download(info);   // 2. Download
manager.install(dl);                  // 3. Install
manager.rollback();                   // 4. Rollback (if needed)
```

### Under the hood

| Component | Windows | Linux |
|-----------|---------|-------|
| HTTP client | WinHTTP (built into OS) | POSIX sockets + OpenSSL |
| JSON parser | Custom (620 lines) | Custom (same code) |
| Semver | `Version::compare` | `Version::compare` |
| Download | `WinHttpReadData` loop | `SSL_read` / `recv` |

**No external dependencies on Windows.** Linux requires `libssl-dev`.

---

## 7. Cross-Platform Programming

### System call equivalents

| Task | Windows | Linux |
|------|---------|-------|
| Load library | `LoadLibraryA("lib.dll")` | `dlopen("lib.so", RTLD_NOW)` |
| Get symbol | `GetProcAddress(h, "fn")` | `dlsym(h, "fn")` |
| Process priority | `SetPriorityClass(h, HIGH)` | `setpriority(PRIO_PROC,0,-20)` |
| Sleep | `Sleep(ms)` | `usleep(ms * 1000)` |
| System RAM | `GlobalMemoryStatusEx` | `sysconf(_SC_PHYS_PAGES)` |
| Process ID | `GetCurrentProcessId()` | `getpid()` |
| Path separator | `\` | `/` |
| Overlay window | Win32 Layered Window | X11 override_redirect |
| HTTP requests | WinHTTP | POSIX sockets + OpenSSL |

### `#ifdef` pattern

```cpp
void myFunction() {
#ifdef _WIN32
    SetPriorityClass(GetCurrentProcess(), HIGH_PRIORITY_CLASS);
#else
    setpriority(PRIO_PROCESS, 0, -20);
#endif
}
```

### CMake pattern

```cmake
if(WIN32)
    target_link_libraries(myaddon PRIVATE winmm psapi)
else()
    find_package(Threads REQUIRED)
    target_link_libraries(myaddon PRIVATE Threads::Threads)
endif()
```

---

## 8. JNI: Java + C++

### Scenario 1: Java calls C++

**Java side** (`MyAddon.java`):
```java
package ravex.addon.myaddon;

public class MyAddon implements Addon {
    static { System.loadLibrary("FeatureAddon"); }
    public static native void nativeLog(String message);
    public static native int  nativeGetPid();
}
```

**C++ side** (`main.cpp`):
```cpp
extern "C" JNIEXPORT void JNICALL
Java_ravex_addon_myaddon_MyAddon_nativeLog(JNIEnv* env, jclass, jstring msg) {
    const char* str = env->GetStringUTFChars(msg, nullptr);
    std::cout << "[Native] " << str << std::endl;
    env->ReleaseStringUTFChars(msg, str);
}
```

### Scenario 2: C++ calls Java (Callback)

```cpp
static JavaVM* g_jvm = nullptr;

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_jvm = vm;
    return JNI_VERSION_1_8;
}

void fireEvent(const char* data) {
    JNIEnv* env;
    g_jvm->AttachCurrentThread((void**)&env, nullptr);
    env->CallVoidMethod(g_obj, g_callback, env->NewStringUTF(data));
}
```

### JniBridge.hpp helpers

In `02_features/JniBridge.hpp`:

```cpp
std::string str = jstring2str(env, javaString);
jstring js = str2jstring(env, "Hello from C++");
JNIEnv* env = getEnv();                       // Safe: attaches if needed
fireCallback("onData", "(Ljava/lang/String;)V", arg);
```

---

## 9. Building & Installing

### Quick build (scripts)

```bash
# Linux
cd templates/cpp/scripts
chmod +x build.sh
./build.sh                       # Builds 02_features
./build.sh 01_minimal            # Builds 01_minimal
./build.sh 04_github --install   # Build & install
```

```cmd
REM Windows
cd templates\cpp\scripts
build.bat                        % Builds 02_features
build.bat 03_overlay             % Builds 03_overlay
build.bat 01_minimal --install   % Build & install
```

### Manual build (any example)

```bash
cd 02_features
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . -j$(nproc)
```

### Installation

Copy the `.dll`/`.so` to the native addons folder:

| Platform | Path |
|----------|------|
| Windows | `%USERPROFILE%\.minecraft\ravex\addons\native\` |
| Linux | `~/.minecraft/ravex/addons/native/` |

Scripts do this automatically with `--install`.

---

## 10. API Reference

### RaveX Addon API (`src/main/cpp/addon/include/`)

| Header | Class/Macros | Purpose |
|--------|-------------|---------|
| `Addon.h` | `class Addon` | Base class (onLoad, onUnload, getName, getVersion) |
| `AddonContext.h` | `class AddonContext` | Context (logInfo, getAddonName, getDataDir) |
| `AddonConfig.h` | `class AddonConfig` | Key-value config (set, get, save, load) |
| `AddonEvent.h` | `class AddonEvent` | Event (getName, isCancelled, setCancelled) |
| `AddonListener.h` | `class AddonListener` | Event listener (onEvent) |
| `AddonRegistry.h` | `class AddonRegistry` | Addon registry (registerAddon, findAddon) |
| `AddonThread.h` | `class AddonMutex` | Mutex wrapper (lock, unlock) |
| `AddonMath.h` | `clamp`, `lerp` | Math utilities |
| `AddonVersion.h` | `getApiVersion`, `isCompatible` | API version check |
| `AddonLogger.h` | `class AddonLogger` | Logging (info, warn, error) |
| `AddonMeta.h` | `class AddonMeta` | Addon metadata |
| `SystemUtils.h` | `getMinecraftDir`, `getAddonsDir` | File utilities |

### platform.hpp macros

| Macro | Windows | Linux |
|-------|---------|-------|
| `ADDON_API` | `__declspec(dllexport)` | `__attribute__((visibility("default")))` |
| `ADDON_SLEEP(ms)` | `Sleep(ms)` | `usleep(ms*1000)` |
| `ADDON_LOAD_LIB(n)` | `LoadLibraryA(n)` | `dlopen(n, RTLD_NOW)` |
| `ADDON_SET_HIGH_PRIORITY()` | `SetPriorityClass(h, HIGH)` | `setpriority(PRIO_PROC,0,-20)` |
| `ADDON_SET_NORMAL_PRIORITY()` | `SetPriorityClass(h, NORMAL)` | `setpriority(PRIO_PROC,0,0)` |
| `ADDON_TRIM_MEMORY()` | `EmptyWorkingSet(h)` | `malloc_trim(0)` |
| `ADDON_GET_TOTAL_RAM_MB()` | `GlobalMemoryStatusEx` | `sysconf(_SC_PHYS_PAGES)` |
| `ADDON_PATH_SEP` | `'\\'` | `'/'` |

### GitHub Tools API (`src/main/cpp/plugins/github/include/`)

| Class | Methods | Purpose |
|-------|---------|---------|
| `Version` | `fromString`, `toString`, `compare` | Semver version |
| `ReleaseChecker` | `checkForUpdates`, `listReleases`, `getLatestRelease` | Update checking |
| `ReleaseManager` | `check`, `download`, `install`, `rollback` | Full update workflow |
| `HttpClient` | `get`, `post`, `download`, `setToken` | HTTP requests |
| `JsonValue` | `parse`, `serialize`, `operator[]`, `asString` | JSON parser |

---

## Next Steps

1. **Start with `01_minimal`** — understand the minimal structure
2. **Study `02_features`** — explore platform.hpp and JNI
3. **Experiment with `03_overlay`** — if you need rendering
4. **Use `04_github`** — if you need auto-updates
5. **Read `src/main/cpp/addon/include/`** — full API for your addon
