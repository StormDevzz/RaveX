// ══════════════════════════════════════════════════════════════════════════════
//  lua/main/LuaAPI.cpp
//
//  RU: Основной API для инициализации Lua-подсистемы RaveX.
//      Содержит функции для запуска Lua-аддонов, интеграции
//      с жизненным циклом RaveX (onLoad/onUnload аддонов),
//      а также тиков и событий.
//
//  EN: Main API for initializing the RaveX Lua subsystem.
//      Contains functions for starting Lua addons, integrating
//      with the RaveX lifecycle (addon onLoad/onUnload),
//      as well as ticks and events.
// ══════════════════════════════════════════════════════════════════════════════

#include "LuaLoader.h"
#include "LuaRegistry.h"
#include "LuaAddon.h"
#include <AddonContext.h>
#include <string>
#include <vector>
#include <memory>
#include <cstdio>

// RU: Функции из LuaEvent.cpp.
// EN: Functions from LuaEvent.cpp.
namespace ravex { namespace lua {
    void dispatchLuaEvent(const std::string& eventName);
}}

// RU: Функции из LuaModule.cpp.
// EN: Functions from LuaModule.cpp.
namespace ravex { namespace lua {
    std::vector<std::pair<std::string, std::string>> getLuaModules();
}}
#include <cstdio>
#include <memory>

namespace ravex {
namespace lua {

// RU: Глобальный экземпляр загрузчика Lua-аддонов.
// EN: Global Lua addon loader instance.
static std::unique_ptr<LuaLoader> s_loader;

// RU: Инициализирует Lua-подсистему.
//     Создаёт загрузчик, сканирует папку addons/lua/,
//     загружает все найденные .lua скрипты.
//     Вызывается из C++ при старте RaveX.
//     Возвращает количество загруженных аддонов.
// EN: Initializes the Lua subsystem.
//     Creates a loader, scans the addons/lua/ folder,
//     loads all found .lua scripts.
//     Called from C++ on RaveX startup.
//     Returns the number of loaded addons.
int initLuaAddons(ravex::addon::AddonContext* ctx) {
    if (s_loader) {
        if (ctx) {
            ctx->logInfo("[LuaAPI] Lua subsystem already initialized");
        }
        return (int)s_loader->count();
    }

    if (ctx) {
        ctx->logInfo("[LuaAPI] Initializing Lua addon subsystem...");
    }

    // RU: Создаём загрузчик и загружаем все аддоны.
    // EN: Create loader and load all addons.
    s_loader = std::make_unique<LuaLoader>("addons/lua");

    // RU: Опционально можно добавить дополнительные пути поиска.
    // EN: Optionally add additional search paths.
    // s_loader->addSearchPath("~/.minecraft/ravex/addons/lua");

    int count = s_loader->loadAll(ctx);

    if (ctx) {
        ctx->logInfo("[LuaAPI] Lua subsystem ready. "
                     + std::to_string(count) + " addon(s) loaded");
    }

    return count;
}

// RU: Останавливает Lua-подсистему. Выгружает все Lua-аддоны.
//     Вызывается при завершении работы RaveX.
// EN: Shuts down the Lua subsystem. Unloads all Lua addons.
//     Called on RaveX shutdown.
void shutdownLuaAddons() {
    if (s_loader) {
        s_loader->unloadAll();
        s_loader.reset();
    }
    LuaRegistry::getInstance().clear();
}

// RU: Вызывает onTick у всех активных Lua-аддонов.
//     Должен вызываться каждый игровой тик из C++.
// EN: Calls onTick on all active Lua addons.
//     Should be called each game tick from C++.
void tickLuaAddons() {
    LuaRegistry::getInstance().tickAll();
}

// RU: Отправляет событие всем Lua-аддонам.
//     Вызывается из C++ при возникновении системных событий.
// EN: Dispatches an event to all Lua addons.
//     Called from C++ when system events occur.
void eventLuaAddons(const std::string& eventName) {
    dispatchLuaEvent(eventName);
}

// RU: Проверяет, инициализирована ли Lua-подсистема.
// EN: Checks whether the Lua subsystem is initialized.
bool isLuaInitialized() {
    return s_loader != nullptr;
}

// RU: Возвращает количество загруженных Lua-аддонов.
// EN: Returns the number of loaded Lua addons.
int getLuaAddonCount() {
    return s_loader ? (int)s_loader->count() : 0;
}

// RU: Загружает один Lua-файл как аддон.
//     Полезно для динамической загрузки отдельных скриптов.
//     Возвращает true при успехе.
// EN: Loads a single Lua file as an addon.
//     Useful for dynamic loading of individual scripts.
//     Returns true on success.
bool loadLuaAddonFile(const std::string& filePath,
                       ravex::addon::AddonContext* ctx) {
    if (!s_loader) {
        s_loader = std::make_unique<LuaLoader>("addons/lua");
    }
    return s_loader->loadFile(filePath, ctx);
}

// RU: Выгружает Lua-аддон по имени.
// EN: Unloads a Lua addon by name.
bool unloadLuaAddon(const std::string& name) {
    auto* addon = LuaRegistry::getInstance().findAddon(name);
    if (!addon) return false;

    LuaRegistry::getInstance().unregisterAddon(name);
    addon->unload();
    return true;
}

// RU: Возвращает список имён всех загруженных Lua-аддонов.
// EN: Returns a list of names of all loaded Lua addons.
std::vector<std::string> listLuaAddons() {
    std::vector<std::string> names;
    auto addons = LuaRegistry::getInstance().getAllAddons();
    for (auto* addon : addons) {
        if (addon) {
            names.push_back(addon->getMeta().name);
        }
    }
    return names;
}

} // namespace lua
} // namespace ravex
