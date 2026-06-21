# RaveX Java Addon Templates

Cross-platform Java addon templates for RaveX (Windows + Linux).

## Quick Start

```bash
cd scripts
./build.sh --install      # Linux: build + install to .minecraft/
```

```cmd
cd scripts
build.bat --install       % Windows: build + install
```

## Files

| File | Description |
|------|-------------|
| [MainAddon.java](src/ravex/addon/template/MainAddon.java) | Main addon: platform detection, native lib loading, module registration |
| [DemoModule.java](src/ravex/addon/template/DemoModule.java) | Module with BooleanParameter, NumberParameter, platform branches |
| [MANIFEST.MF](src/META-INF/MANIFEST.MF) | JAR manifest with Addon-Main-Class |

## Documentation

- [GUIDE.md](GUIDE.md) — Full step-by-step tutorial (English)
- [GUIDE_RU.md](GUIDE_RU.md) — Полное пошаговое руководство (русский)

## Structure

```
templates/java/
├── src/
│   ├── ravex/addon/template/
│   │   ├── MainAddon.java     # Main addon class
│   │   └── DemoModule.java    # Example module
│   └── META-INF/
│       └── MANIFEST.MF        # JAR manifest
├── scripts/
│   ├── build.bat              # Windows build script
│   └── build.sh               # Linux build script
├── GUIDE.md                   # English tutorial
├── GUIDE_RU.md                # Russian tutorial
└── README.md                  # This file
```
