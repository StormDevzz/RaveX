#pragma once

#include "LuaAddon.h"
#include <string>
#include <vector>
#include <memory>

namespace ravex {
namespace addon {
    class AddonContext;
}

namespace lua {

// RU: LuaLoader — сканирует директории на наличие .lua-скриптов
//     и создаёт из них экземпляры LuaAddon. Каждый скрипт
//     становится отдельным аддоном.
//
//     Процесс загрузки:
//       1. Сканирование папки addons/lua/ на *.lua файлы
//       2. Для каждого файла — вызов LuaAddon::loadFromFile
//       3. Проверка, что скрипт определил обязательные функции
//       4. Если успешно — регистрация в LuaRegistry
//       5. Вызов onLoad для каждого загруженного аддона
//
// EN: LuaLoader — scans directories for .lua scripts
//     and creates LuaAddon instances from them. Each script
//     becomes a separate addon.
//
//     Loading process:
//       1. Scan addons/lua/ folder for *.lua files
//       2. For each file — call LuaAddon::loadFromFile
//       3. Verify the script defined required functions
//       4. If successful — register in LuaRegistry
//       5. Call onLoad for each loaded addon
class LuaLoader {
private:
    // RU: Список загруженных Lua-аддонов.
    // EN: List of loaded Lua addons.
    std::vector<std::unique_ptr<LuaAddon>> m_addons;

    // RU: Директория, в которой ищем .lua файлы.
    // EN: Directory where .lua files are searched.
    std::string m_addonDir;

public:
    // RU: Создаёт загрузчик для указанной директории.
    //     По умолчанию — addons/lua/ относительно рабочей папки.
    // EN: Creates a loader for the specified directory.
    //     Default — addons/lua/ relative to working directory.
    explicit LuaLoader(const std::string& dir = "addons/lua");

    ~LuaLoader();

    // RU: Сканирует директорию и загружает все .lua скрипты.
    //     Для каждого скрипта создаётся LuaAddon, вызывается
    //     loadFromFile, затем onLoad. Возвращает количество
    //     успешно загруженных аддонов.
    //     Ошибки логируются через ctx->logInfo/logWarn.
    // EN: Scans the directory and loads all .lua scripts.
    //     For each script a LuaAddon is created, loadFromFile
    //     is called, then onLoad. Returns the number of
    //     successfully loaded addons.
    //     Errors are logged via ctx->logInfo/logWarn.
    int loadAll(ravex::addon::AddonContext* ctx);

    // RU: Загружает один конкретный .lua файл как аддон.
    // EN: Loads a single .lua file as an addon.
    bool loadFile(const std::string& filePath,
                  ravex::addon::AddonContext* ctx);

    // RU: Выгружает все Lua-аддоны (вызывает onUnload у каждого).
    // EN: Unloads all Lua addons (calls onUnload on each).
    void unloadAll();

    // RU: Вызывает onTick у всех загруженных и активных Lua-аддонов.
    // EN: Calls onTick on all loaded and active Lua addons.
    void tickAll();

    // RU: Вызывает onEvent у всех Lua-аддонов, у которых есть
    //     функция onEvent.
    // EN: Calls onEvent on all Lua addons that have an onEvent function.
    void eventAll(const std::string& eventName);

    // RU: Возвращает количество загруженных аддонов.
    // EN: Returns the number of loaded addons.
    size_t count() const { return m_addons.size(); }

    // RU: Получает аддон по индексу.
    // EN: Gets an addon by index.
    LuaAddon* getAddon(size_t index) const;

    // RU: Ищет аддон по имени (из LuaAddonMeta::name).
    //     Возвращает nullptr, если не найден.
    // EN: Finds an addon by name (from LuaAddonMeta::name).
    //     Returns nullptr if not found.
    LuaAddon* findAddon(const std::string& name) const;

private:
    // RU: Собирает все .lua файлы в директории (рекурсивно).
    // EN: Collects all .lua files in the directory (recursively).
    std::vector<std::string> scanDirectory();
};

} // namespace lua
} // namespace ravex
