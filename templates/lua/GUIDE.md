# RaveX Lua Addons — Complete Guide

> Version: 1.0 | Platforms: Windows 10/11, Linux | Language: LuaJIT 2.1 / Lua 5.1

## Table of Contents

1. [What is a Lua Addon?](#1-what-is-a-lua-addon)
2. [How Lua Addons Work](#2-how-lua-addons-work)
3. [01 Minimal Addon](#3-01-minimal-addon)
4. [02 Full-Featured Addon](#4-02-full-featured-addon)
5. [API Reference](#5-api-reference)
6. [Installation](#6-installation)

---

## 1. What is a Lua Addon?

A **Lua addon** is a `.lua` script that RaveX loads at runtime via its embedded Lua engine. Unlike Java addons (JAR) or C++ addons (DLL/SO), Lua addons require no compilation — just write the script and drop it in the addons folder.

### Lua vs Java vs C++

| Aspect | Lua Addon | Java Addon | C++ Native Addon |
|--------|-----------|------------|------------------|
| Complexity | Very Low | Low | High |
| Compilation needed | No | Yes (JAR) | Yes (DLL/SO) |
| Performance | Medium | Medium | High |
| System API access | Limited (sandboxed) | Limited | Full |
| Hot-reload | Yes | No | No |
| File size | ~1 KB | ~2 KB JAR | ~50-500 KB DLL/SO |

### When to use Lua?

- **Quick prototyping**: test ideas without compilation
- **Simple modules**: toggles, chat commands, GUI tweaks
- **Configuration**: dynamic scripts that read/write settings
- **Event-driven logic**: react to game events with minimal code
- **User scripts**: let users customize behavior without C++/Java

---

## 2. How Lua Addons Work

### Architecture

```
┌─────────────────────────────────────────────┐
│                  RaveX Core                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ Java     │  │ C++      │  │ Lua      │   │
│  │ Addons   │  │ Addons   │  │ Engine   │   │
│  │ (JAR)    │  │ (DLL/SO) │  │ (C API)  │   │
│  └──────────┘  └──────────┘  └─────┬────┘   │
│                                    │         │
│                           ┌────────▼──────┐  │
│                           │  Lua Scripts   │  │
│                           │  addons/lua/   │  │
│                           └───────────────┘  │
└─────────────────────────────────────────────┘
```

### Lifecycle

1. RaveX starts → C++ Lua subsystem initializes
2. Scans `addons/lua/` directory for `.lua` files
3. For each file:
   - Creates a sandboxed Lua environment
   - Loads and executes the script
   - Calls `getName()` and `getVersion()` for metadata
   - Calls `onLoad(ctx)` — addon is now active
4. During gameplay:
   - Calls `onTick()` each game tick
   - Calls `onEvent(eventName)` when events occur
5. On shutdown:
   - Calls `onUnload()` for each addon
   - Destroys the Lua environment

### Sandbox

For security, Lua scripts run in a sandboxed environment:
- `os.execute`, `os.exit`, `io.*` are blocked
- `dofile`, `loadfile`, `require` are blocked
- Only RaveX API functions and safe Lua standard libraries are available

---

## 3. 01 Minimal Addon

**Location:** `01_minimal/main.lua`

This is the simplest possible Lua addon. Every addon must define these functions:

```lua
function getName()
    return "MyFirstAddon"
end

function getVersion()
    return "1.4.2"
end

function onLoad(ctx)
    logInfo("Hello from Lua!")
end

function onUnload()
    logInfo("Goodbye from Lua!")
end
```

### What happens

1. RaveX loads `main.lua` from `addons/lua/MyFirstAddon/`
2. Calls `getName()` → "MyFirstAddon"
3. Calls `getVersion()` → "1.4.2"
4. Calls `onLoad(ctx)` → logs "Hello from Lua!"
5. Addon is now running

---

## 4. 02 Full-Featured Addon

**Location:** `02_features/main.lua`

Demonstrates config, modules, events, ticks, and platform branching.

### Config

```lua
-- Read a value (with default)
local enabled = configGet("enabled", "false")

-- Write a value
configSet("last_loaded", "MyAddon v1.4.2")
```

Config values persist between game sessions.

### Modules

```lua
-- Register a toggleable module in the GUI
registerModule("MyModule", "Description of my module")

-- Check if the user enabled it
if isModuleEnabled("MyModule") then
    logInfo("Module is active!")
end
```

### Events

```lua
function onEvent(eventName)
    if eventName == "world_load" then
        logInfo("World loaded!")
    end
end
```

### Ticks

```lua
local counter = 0
function onTick()
    counter = counter + 1
    if counter % 100 == 0 then
        logInfo("Tick #" .. counter)
    end
end
```

### Platform branching

```lua
if getPlatform() == "windows" then
    -- Windows-specific logic
else
    -- Linux-specific logic
end
```

---

## 5. API Reference

### Logging

| Function | Description |
|----------|-------------|
| `logInfo(msg)` | Log an info message |
| `logWarn(msg)` | Log a warning |
| `logError(msg)` | Log an error |

### Config

| Function | Description |
|----------|-------------|
| `configGet(key, default)` | Get config value (string) |
| `configSet(key, value)` | Set config value (string) |

### Modules

| Function | Description |
|----------|-------------|
| `registerModule(name, description)` | Register a module in the GUI |
| `isModuleEnabled(name)` | Check if a module is enabled |

### System

| Function | Description |
|----------|-------------|
| `getPlatform()` | Returns "windows" or "linux" |
| `getApiVersion()` | Returns RaveX API version string |
| `getAddonDir()` | Returns the addons/lua directory path |

### Addon callbacks (define in your script)

| Function | Required | Description |
|----------|----------|-------------|
| `getName()` | Yes | Returns unique addon name |
| `getVersion()` | Yes | Returns semver version string |
| `onLoad(ctx)` | Yes | Called on addon load |
| `onUnload()` | Yes | Called on addon unload |
| `onTick()` | No | Called each game tick |
| `onEvent(name)` | No | Called on system events |
| `getAuthor()` | No | Returns author name |
| `getDescription()` | No | Returns addon description |

---

## 6. Installation

### Windows

```cmd
copy 01_minimal\main.lua "%USERPROFILE%\.minecraft\ravex\addons\lua\"
```

Or create a folder for your addon:

```cmd
mkdir "%USERPROFILE%\.minecraft\ravex\addons\lua\MyAddon"
copy main.lua "%USERPROFILE%\.minecraft\ravex\addons\lua\MyAddon\"
```

### Linux

```bash
cp 01_minimal/main.lua ~/.minecraft/ravex/addons/lua/
mkdir -p ~/.minecraft/ravex/addons/lua/MyAddon
cp main.lua ~/.minecraft/ravex/addons/lua/MyAddon/
```

### Structure

```
~/.minecraft/ravex/addons/lua/
├── MyFirstAddon/
│   └── main.lua
├── MySecondAddon/
│   └── main.lua
└── standalone.lua       # also works directly in lua/
```

RaveX scans the `lua/` folder recursively and loads all `.lua` files.
