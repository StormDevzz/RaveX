# RaveX C Addons - Guide

> Version: 1.0 | Platform: Linux | Language: C23

---

## Contents

1. [What is a C addon?](#1-what-is-a-c-addon)
2. [Project structure](#2-project-structure)
3. [01 Minimal addon](#3-01-minimal-addon)
4. [02 Full-featured addon](#4-02-full-featured-addon)
5. [Build and install](#5-build-and-install)
6. [API reference](#6-api-reference)

---

## 1. What is a C addon?

A **C addon** is a dynamic library (`.so`) that RaveX loads at runtime via `dlopen`. Unlike C++ addons, C addons run **on Linux only** and have no direct JNI access.

### Why C when C++ and Java exist?

| Task | Java | C++ | C |
|--------|------|-----|---|
| Ease of writing | High | Medium | Medium |
| Binary size | ~2 KB JAR | ~50-500 KB .so | ~10-50 KB .so |
| Dependencies | None | STL + maybe libpthread etc. | libc only |
| Cross-platform | Yes | Windows + Linux | Linux only |
| Minimal overhead | No (JVM) | No | No |

C addons are useful when you need a minimal binary without C++ runtime dependencies. For example: custom checks, lightweight utilities, packet senders.

### Limitations

- C addons work **on Linux only**
- C addons have **no direct Java access** - only through the API provided by the loader
- No RAII, no exceptions - manage memory manually

---

## 2. Project structure

```
templates/c/
├── 01_minimal/               # 1. Minimal addon
│   ├── CMakeLists.txt          # Build config
│   └── src/addon.c             # Source code
├── 02_features/              # 2. Addon with tick and key handlers
│   ├── CMakeLists.txt
│   └── src/addon.c
├── scripts/
│   └── build.sh               # Linux build script
├── GUIDE.md                   # This guide
└── README.md
```

---

## 3. 01 Minimal addon

File: `templates/c/01_minimal/src/addon.c`

```c
#include "../../../src/main/c/addon/include/c_addon_api.h"
#include <stdio.h>

static ravex_c_addon_api* g_api = NULL;

int ravex_c_addon_init(ravex_c_addon_api* api) {
    g_api = api;
    if (g_api && g_api->log_info)
        g_api->log_info("MinimalCAddon loaded");
    return 0;
}

void ravex_c_addon_shutdown(void) {
    if (g_api && g_api->log_info)
        g_api->log_info("MinimalCAddon unloaded");
    g_api = NULL;
}

ravex_c_addon_meta ravex_c_addon_meta_info = {
    .api_version = RAVEX_C_ADDON_API_VERSION,
    .name        = "MinimalCAddon",
    .version     = "1.0.0",
    .description = "Minimal C addon",
    .author      = "You"
};
```

### What's happening here?

- `ravex_c_addon_meta_info` - required global struct. The loader reads it via `dlsym` to get the addon name, version and description.
- `ravex_c_addon_init` - called right after loading. Receives a pointer to the API struct with logging functions. Must return 0 on success.
- `ravex_c_addon_shutdown` - called when the addon is unloaded. Free all resources here.

### How it works

1. RaveX finds `.so` files with the `c_addon_` prefix in `RaveX/addons/c_native/`
2. Loads the library via `dlopen`
3. Looks up the `ravex_c_addon_meta_info` symbol, reads metadata
4. Calls `ravex_c_addon_init`, passing the API pointer
5. On shutdown, calls `ravex_c_addon_shutdown` and `dlclose`

---

## 4. 02 Full-featured addon

File: `templates/c/02_features/src/addon.c`

```c
#include "../../../src/main/c/addon/include/c_addon_api.h"
#include <stdio.h>

static ravex_c_addon_api* g_api = NULL;
static int g_tick_count = 0;
static int g_key_presses = 0;

int ravex_c_addon_init(ravex_c_addon_api* api) {
    g_api = api;
    if (g_api && g_api->log_info)
        g_api->log_info("FeatureCAddon loaded");
    return 0;
}

void ravex_c_addon_shutdown(void) {
    if (g_api && g_api->log_info) {
        char buf[128];
        snprintf(buf, sizeof(buf),
            "FeatureCAddon unloaded. Ticks: %d, key presses: %d",
            g_tick_count, g_key_presses);
        g_api->log_info(buf);
    }
    g_api = NULL;
}

void ravex_c_addon_on_tick(void) {
    g_tick_count++;
}

void ravex_c_addon_on_key(int key, int action) {
    if (action == 1) {
        g_key_presses++;
        if (g_api && g_api->log_info) {
            char buf[64];
            snprintf(buf, sizeof(buf), "FeatureCAddon: key %d pressed", key);
            g_api->log_info(buf);
        }
    }
}

ravex_c_addon_meta ravex_c_addon_meta_info = {
    .api_version = RAVEX_C_ADDON_API_VERSION,
    .name        = "FeatureCAddon",
    .version     = "1.0.0",
    .description = "C addon with tick and key handlers",
    .author      = "You"
};
```

### What's new?

- `ravex_c_addon_on_tick` - called every game tick. Optional.
- `ravex_c_addon_on_key` - called on key press/release. `action == 1` is press, `0` is release. Optional.

Both functions are optional. If you don't export them, the loader simply skips them.

---

## 5. Build and install

### Requirements

- Linux
- GCC or Clang
- CMake >= 3.16

### Build an example

```bash
cd templates/c
chmod +x scripts/build.sh
./scripts/build.sh 01_minimal
```

To install directly into Minecraft:

```bash
./scripts/build.sh 01_minimal --install
```

The addon will be at `~/.minecraft/RaveX/addons/c_native/c_addon_minimal.so`.

### Build your own addon

Copy `01_minimal`, rename it, write your code. Make sure the `.so` filename starts with `c_addon_` or the loader won't find it.

```
my_addon/
├── CMakeLists.txt
└── src/addon.c
```

In `CMakeLists.txt`, set:

```cmake
set_target_properties(my_addon PROPERTIES
    OUTPUT_NAME "c_addon_my_addon"
    PREFIX ""
    SUFFIX ".so"
)
```

The `.so` name must start with `c_addon_`.

---

## 6. API reference

### Header file

Path: `src/main/c/addon/include/c_addon_api.h`

Include it from your template:

```c
#include "../../../src/main/c/addon/include/c_addon_api.h"
```

### Structs

```c
typedef struct {
    int         api_version;     // Must be RAVEX_C_ADDON_API_VERSION
    const char* name;            // Addon name
    const char* version;         // Version string
    const char* description;     // Short description
    const char* author;          // Author name
} ravex_c_addon_meta;
```

### API passed to init

```c
typedef struct {
    void (*log_info)(const char* msg);       // Log at info level
    void (*log_warn)(const char* msg);       // Log at warning level
    void (*log_error)(const char* msg);      // Log at error level
    const char* (*get_mc_version)(void);     // Get Minecraft version
    bool (*is_key_down)(int key_code);       // Check if key is held
} ravex_c_addon_api;
```

### Required exported symbols

| Symbol | Type | Description |
|--------|------|-------------|
| `ravex_c_addon_meta_info` | `ravex_c_addon_meta` | Addon metadata (global variable) |
| `ravex_c_addon_init` | `int (*)(ravex_c_addon_api*)` | Initialization, return 0 on success |
| `ravex_c_addon_shutdown` | `void (*)(void)` | Cleanup on unload |

### Optional exported symbols

| Symbol | Type | Description |
|--------|------|-------------|
| `ravex_c_addon_on_tick` | `void (*)(void)` | Called every tick |
| `ravex_c_addon_on_key` | `void (*)(int key, int action)` | Keyboard event |

---

## 7. Signing and Security

All addons (Java, C++, C) must be signed before RaveX loads them. Unsigned addons are rejected.

### How signing works

1. RaveX ships with an embedded RSA-2048 public key
2. Each addon file has a sidecar signature file with the extension `.ravex-sig`
3. The signature is a SHA-256 hash of the addon file, encrypted with the corresponding private key
4. The loader verifies the signature before loading the addon

### Generating a keypair

```bash
cd RaveX
javac -d /tmp/keys src/main/java/ravex/addon/security/KeyGenerator.java \
    src/main/java/ravex/addon/security/AddonSignature.java \
    src/main/java/ravex/addon/security/AddonSigner.java \
    src/main/java/ravex/addon/util/AddonException.java
java -cp /tmp/keys ravex.addon.security.KeyGenerator /path/to/output
```

This creates `public.der` and `private.der`. Copy `public.der` to `src/main/resources/assets/ravex/security/` before building RaveX.

### Signing an addon

```bash
java -cp /tmp/keys ravex.addon.security.AddonSigner \
    /path/to/private.der \
    /path/to/c_addon_my_addon.so
```

Creates `c_addon_my_addon.so.ravex-sig` next to the addon file.

### Sandbox restrictions for Java addons

Java addons are loaded through a `SecureAddonClassLoader` that blocks:

- Reflection APIs (`java.lang.reflect`, `java.lang.invoke`)
- Process execution (`java.lang.Runtime`, `java.lang.ProcessBuilder`)
- File system writes (`java.io.FileOutputStream`, `java.io.FileWriter`, etc.)
- Network access (`java.net.Socket`, `java.net.HttpURLConnection`)
- Security and crypto APIs

C and C++ addons are sandboxed by design:
- C addons have no direct JNI access - only the limited `ravex_c_addon_api` struct
- C++ addons are loaded as separate shared libraries with controlled entry points

### File layout

```
~/.minecraft/RaveX/addons/
├── my_addon.jar              # Java addon
├── my_addon.jar.ravex-sig    # Java addon signature
├── native/
│   ├── MyAddon.so            # C++ addon
│   └── MyAddon.so.ravex-sig  # C++ addon signature
└── c_native/
    ├── c_addon_my.so         # C addon
    └── c_addon_my.so.ravex-sig # C addon signature
```
