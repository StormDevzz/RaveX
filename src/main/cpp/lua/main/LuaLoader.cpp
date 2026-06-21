// ══════════════════════════════════════════════════════════════════════════════
//  lua/main/LuaLoader.cpp
//
//  RU: Реализация LuaLoader — сканирование директорий, загрузка
//      .lua-скриптов как аддонов, управление их жизненным циклом.
//
//  EN: LuaLoader implementation — directory scanning, loading .lua
//      scripts as addons, managing their lifecycle.
// ══════════════════════════════════════════════════════════════════════════════

#include "LuaLoader.h"
#include "LuaRegistry.h"
#include "LuaScript.h"
#include <AddonContext.h>
#include <AddonConfig.h>
#include <algorithm>
#include <cstdio>
#include <cstring>
#ifdef __linux__
#include <dirent.h>
#endif
#include <fstream>
#include <sstream>
#include <system_error>

namespace ravex {
namespace lua {

LuaLoader::LuaLoader(const std::string& dir) : m_addonDir(dir) {
}

LuaLoader::~LuaLoader() {
    unloadAll();
}

int LuaLoader::loadAll(ravex::addon::AddonContext* ctx) {
    auto files = scanDirectory();
    int loaded = 0;

    for (const auto& file : files) {
        if (loadFile(file, ctx)) {
            loaded++;
        }
    }

    if (ctx) {
        ctx->logInfo("[LuaLoader] Loaded " + std::to_string(loaded)
                     + " / " + std::to_string(files.size()) + " Lua addons");
    }
    return loaded;
}

bool LuaLoader::loadFile(const std::string& filePath,
                          ravex::addon::AddonContext* ctx) {
    auto addon = std::make_unique<LuaAddon>();

    // RU: Создаём конфиг для Lua-аддона.
    // EN: Create a config for the Lua addon.
    auto* cfg = new ravex::addon::AddonConfig();

    auto result = addon->loadFromFile(filePath, ctx, cfg);

    if (!result.success) {
        if (ctx) {
            ctx->logInfo("[LuaLoader] Failed: " + filePath
                         + " - " + result.errorMsg);
        }
        delete cfg;
        return false;
    }

    // RU: Вызываем onLoad скрипта.
    // EN: Call the script's onLoad.
    addon->callOnLoad();

    // RU: Регистрируем в реестре.
    // EN: Register in the registry.
    LuaRegistry::getInstance().registerAddon(addon.get());

    if (ctx) {
        ctx->logInfo("[LuaLoader] Loaded: " + addon->getMeta().name
                     + " v" + addon->getMeta().version
                     + " from " + filePath);
    }

    m_addons.push_back(std::move(addon));
    return true;
}

void LuaLoader::unloadAll() {
    for (auto& addon : m_addons) {
        LuaRegistry::getInstance().unregisterAddon(addon->getMeta().name);
        addon->unload();
    }
    m_addons.clear();
}

void LuaLoader::tickAll() {
    for (auto& addon : m_addons) {
        addon->callOnTick();
    }
}

void LuaLoader::eventAll(const std::string& eventName) {
    for (auto& addon : m_addons) {
        addon->callOnEvent(eventName);
    }
}

LuaAddon* LuaLoader::getAddon(size_t index) const {
    if (index < m_addons.size()) {
        return m_addons[index].get();
    }
    return nullptr;
}

LuaAddon* LuaLoader::findAddon(const std::string& name) const {
    for (const auto& addon : m_addons) {
        if (addon->getMeta().name == name) {
            return addon.get();
        }
    }
    return nullptr;
}

std::vector<std::string> LuaLoader::scanDirectory() {
    std::vector<std::string> files;

    // RU: Используем SystemUtils::listFiles, если доступно.
    //     Иначе — простая проверка существования аддона по пути.
    // EN: Use SystemUtils::listFiles if available.
    //     Otherwise — simple check that the addon path exists.
    // RU: В реальной реализации здесь рекурсивный обход директории.
    //     Для примера проверяем одну директорию.
    // EN: In a real implementation this would recursively scan the directory.
    //     For this example we check a single directory.
#ifdef _WIN32
    // RU: На Windows используем FindFirstFile/FindNextFile.
    // EN: On Windows use FindFirstFile/FindNextFile.
    std::string pattern = m_addonDir + "\\*.lua";
    WIN32_FIND_DATAA ffd;
    HANDLE hFind = FindFirstFileA(pattern.c_str(), &ffd);
    if (hFind != INVALID_HANDLE_VALUE) {
        do {
            if (!(ffd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)) {
                files.push_back(m_addonDir + "\\" + ffd.cFileName);
            }
        } while (FindNextFileA(hFind, &ffd) != 0);
        FindClose(hFind);
    }
#else
    // RU: На Linux используем вызов ls или opendir/readdir.
    // EN: On Linux use opendir/readdir.
    DIR* dir = opendir(m_addonDir.c_str());
    if (dir) {
        struct dirent* entry;
        while ((entry = readdir(dir)) != nullptr) {
            std::string name = entry->d_name;
            if (name.size() > 4 &&
                name.substr(name.size() - 4) == ".lua") {
                files.push_back(m_addonDir + "/" + name);
            }
        }
        closedir(dir);
    }
#endif

    // RU: Сортируем для детерминированного порядка загрузки.
    // EN: Sort for deterministic load order.
    std::sort(files.begin(), files.end());
    return files;
}

} // namespace lua
} // namespace ravex
