-- ════════════════════════════════════════════════════════════════════════════
--  01_minimal / main.lua
--
--  RU: Минимальный Lua-аддон для RaveX. Обязательный минимум:
--        getName()    -> string  — уникальное имя аддона
--        getVersion() -> string  — версия в формате semver
--        onLoad(ctx)  -> nil     — инициализация (вызывается при загрузке)
--        onUnload()   -> nil     — очистка (вызывается при выгрузке)
--
--      RaveX сам находит этот файл в папке addons/lua/ и загружает его.
--      Все функции API доступны как глобальные: logInfo, logWarn и т.д.
--
--  EN: Minimal Lua addon for RaveX. Required minimum:
--        getName()    -> string  — unique addon name
--        getVersion() -> string  — semver version string
--        onLoad(ctx)  -> nil     — initialization (called on load)
--        onUnload()   -> nil     — cleanup (called on unload)
--
--      RaveX finds this file in addons/lua/ and loads it automatically.
--      All API functions are available globally: logInfo, logWarn, etc.
-- ════════════════════════════════════════════════════════════════════════════

-- RU: Возвращает имя аддона (уникальное, без пробелов).
-- EN: Returns the addon name (unique, no spaces).
function getName()
    return "MinimalLuaAddon"
end

-- RU: Возвращает версию в формате major.minor.patch (semver).
-- EN: Returns version in major.minor.patch format (semver).
function getVersion()
    return "1.4.2"
end

-- RU: Опционально: автор аддона.
-- EN: Optional: addon author.
function getAuthor()
    return "RaveX Team"
end

-- RU: Опционально: описание аддона.
-- EN: Optional: addon description.
function getDescription()
    return "Minimal Lua addon example for RaveX"
end

-- RU: Вызывается при загрузке аддона. Здесь ты можешь:
--   - логировать информацию (logInfo, logWarn, logError)
--   - читать/писать конфиг (configGet, configSet)
--   - узнавать платформу (getPlatform)
--   - регистрировать модули (registerModule)
--   - подписываться на события (registerEvent)
-- EN: Called when the addon is loaded. Here you can:
--   - log information (logInfo, logWarn, logError)
--   - read/write config (configGet, configSet)
--   - check platform (getPlatform)
--   - register modules (registerModule)
--   - subscribe to events (registerEvent)
function onLoad(ctx)
    logInfo("MinimalLuaAddon загружается...")

    local platform = getPlatform()
    logInfo("Platform: " .. platform)

    local apiVer = getApiVersion()
    logInfo("RaveX API version: " .. apiVer)

    logInfo("MinimalLuaAddon загружен!")
end

-- RU: Вызывается при выгрузке аддона. Освободи ресурсы, если нужно.
-- EN: Called when the addon is unloaded. Free resources if needed.
function onUnload()
    logInfo("MinimalLuaAddon выгружен.")
end
