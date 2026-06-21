# RaveX Lua Addon Templates

Cross-platform Lua addon templates for RaveX (Windows + Linux).

## Quick Start

Place your .lua files in the `addons/lua/` folder inside your RaveX installation directory. RaveX will automatically load them on startup.

### Manual copy

```bash
cp 01_minimal/main.lua ~/.minecraft/ravex/addons/lua/
```

```cmd
copy 01_minimal\main.lua "%USERPROFILE%\.minecraft\ravex\addons\lua\"
```

## Examples

| # | Directory | What it teaches |
|---|-----------|-----------------|
| 01 | [01_minimal/](01_minimal/) | Minimal addon: getName, getVersion, onLoad, onUnload |
| 02 | [02_features/](02_features/) | Full addon: config, modules, events, ticks, platform branching |

## Documentation

- [GUIDE.md](GUIDE.md) — Full step-by-step tutorial (English)
- [GUIDE_RU.md](GUIDE_RU.md) — Полное пошаговое руководство (русский)
