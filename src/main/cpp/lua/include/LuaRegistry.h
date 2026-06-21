#pragma once

#include "LuaAddon.h"
#include <string>
#include <unordered_map>
#include <memory>

namespace ravex {
namespace lua {

// RU: LuaRegistry — реестр всех загруженных Lua-аддонов.
//     Позволяет искать аддоны по имени, получать список всех
//     аддонов, управлять их жизненным циклом.
//
//     Реестр — синглтон (один на всё приложение). Получить
//     экземпляр можно через Registry::getInstance().
//
// EN: LuaRegistry — registry of all loaded Lua addons.
//     Allows searching addons by name, getting a list of all
//     addons, managing their lifecycle.
//
//     Registry is a singleton (one per application). Get the
//     instance via Registry::getInstance().
class LuaRegistry {
private:
    // RU: Карта имя-аддон для быстрого поиска.
    // EN: Name-to-addon map for fast lookup.
    std::unordered_map<std::string, LuaAddon*> m_addonMap;

    // RU: Список всех аддонов в порядке загрузки.
    // EN: List of all addons in load order.
    std::vector<LuaAddon*> m_addonList;

    // RU: Приватный конструктор (синглтон).
    // EN: Private constructor (singleton).
    LuaRegistry() = default;

public:
    // RU: Синглтон — получить единственный экземпляр.
    // EN: Singleton — get the single instance.
    static LuaRegistry& getInstance();

    // RU: Запрещаем копирование.
    // EN: Disable copying.
    LuaRegistry(const LuaRegistry&) = delete;
    LuaRegistry& operator=(const LuaRegistry&) = delete;

    // RU: Регистрирует аддон в реестре.
    //     Если аддон с таким именем уже есть — перезаписывает.
    // EN: Registers an addon in the registry.
    //     If an addon with the same name exists — overwrites.
    void registerAddon(LuaAddon* addon);

    // RU: Удаляет аддон из реестра по имени.
    // EN: Removes an addon from the registry by name.
    void unregisterAddon(const std::string& name);

    // RU: Ищет аддон по имени. Возвращает nullptr, если не найден.
    // EN: Finds an addon by name. Returns nullptr if not found.
    LuaAddon* findAddon(const std::string& name) const;

    // RU: Возвращает список всех зарегистрированных аддонов.
    // EN: Returns a list of all registered addons.
    std::vector<LuaAddon*> getAllAddons() const;

    // RU: Возвращает количество зарегистрированных аддонов.
    // EN: Returns the number of registered addons.
    size_t count() const { return m_addonMap.size(); }

    // RU: Очищает реестр (не удаляет аддоны, только ссылки).
    // EN: Clears the registry (does not delete addons, only references).
    void clear();

    // RU: Вызывает onTick у всех зарегистрированных аддонов.
    // EN: Calls onTick on all registered addons.
    void tickAll();

    // RU: Вызывает onEvent у всех аддонов с функцией onEvent.
    // EN: Calls onEvent on all addons with an onEvent function.
    void eventAll(const std::string& eventName);
};

} // namespace lua
} // namespace ravex
