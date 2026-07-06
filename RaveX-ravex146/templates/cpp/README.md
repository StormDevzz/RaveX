# RaveX C++ Native Addon Templates

Cross-platform native addon templates for RaveX (Windows + Linux).

## Quick Start

```bash
cd scripts
./build.sh               # Linux: build default example (02_features)
./build.sh --install      # Linux: build + install to .minecraft/
```

```cmd
cd scripts
build.bat                 % Windows: build 02_features
build.bat --install       % Windows: build + install
```

## Examples

| # | Directory | What it teaches |
|---|-----------|-----------------|
| 01 | [01_minimal/](01_minimal/) | Minimal addon: onLoad/onUnload, createAddon/destroyAddon |
| 02 | [02_features/](02_features/) | Full addon: config, events, threads, platform API, JNI bridge |
| 03 | [03_overlay/](03_overlay/) | Overlay window: Win32 layered window (GDI) + X11 override-redirect |
| 04 | [04_github/](04_github/) | GitHub auto-update: ReleaseChecker, ReleaseManager, HTTP client |

## Documentation

- [GUIDE.md](GUIDE.md) — Full step-by-step tutorial (English, ~300 lines)
- [GUIDE_RU.md](GUIDE_RU.md) — Полное пошаговое руководство (русский)

## Structure

```
templates/cpp/
├── 01_minimal/          Self-contained minimal addon
├── 02_features/         Self-contained full addon + JniBridge + platform.hpp
├── 03_overlay/          Self-contained Win32/X11 overlay
├── 04_github/           Self-contained GitHub updater
├── scripts/
│   ├── build.bat        Build any example by name
│   └── build.sh         Build any example by name
├── GUIDE.md             English tutorial
├── GUIDE_RU.md          Russian tutorial
└── README.md            This file
```

Each example is a **standalone CMake project** — build it independently:

```bash
cd 01_minimal && mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build .
```
