-- ════════════════════════════════════════════════════════════════════════════
--  02_features / main.lua
--
--  RU: Полнофункциональный Lua-аддон для RaveX. Демонстрирует:
--        1. Логирование (logInfo, logWarn, logError)
--        2. Конфиг (configGet, configSet)
--        3. Регистрацию модулей (registerModule)
--        4. Обработку событий (onEvent)
--        5. Тики (onTick)
--        6. Платформенные ветки (getPlatform)
--
--  EN: Full-featured Lua addon for RaveX. Demonstrates:
--        1. Logging (logInfo, logWarn, logError)
--        2. Config (configGet, configSet)
--        3. Module registration (registerModule)
--        4. Event handling (onEvent)
--        5. Ticks (onTick)
--        6. Platform branching (getPlatform)
-- ════════════════════════════════════════════════════════════════════════════

local tickCount = 0
local configEnabled = false

-- RU: Метаданные аддона.
-- EN: Addon metadata.
function getName()
    return "FeatureLuaAddon"
end

function getVersion()
    return "1.4.3"
end

function getAuthor()
    return "RaveX Team"
end

-- RU: Инициализация аддона.
-- EN: Addon initialization.
function onLoad(ctx)
    logInfo("FeatureLuaAddon: загрузка...")

    -- RU: Определяем платформу.
    -- EN: Detect platform.
    local platform = getPlatform()
    logInfo("Platform: " .. platform)

    -- RU: Читаем конфиг. Если ключа нет - используем значение по умолчанию.
    -- EN: Read config. If key is missing - use default value.
    local enabled = configGet("enabled", "false")
    configEnabled = (enabled == "true")
    logInfo("Config enabled: " .. enabled)

    -- RU: Записываем в конфиг (сохраняется автоматически).
    -- EN: Write to config (saved automatically).
    configSet("last_loaded", "FeatureLuaAddon v1.4.3")

    -- RU: Регистрируем модуль, который появится в GUI.
    -- EN: Register a module that will appear in the GUI.
    registerModule("LuaFeatureModule", "Example Lua module with settings")

    -- RU: Проверяем, включён ли наш модуль.
    -- EN: Check if our module is enabled.
    if isModuleEnabled("LuaFeatureModule") then
        logInfo("LuaFeatureModule is enabled")
    end

    logInfo("FeatureLuaAddon загружен!")
end

-- RU: Выгрузка аддона.
-- EN: Addon unload.
function onUnload()
    logInfo("FeatureLuaAddon выгружен.")
end

-- RU: Вызывается каждый игровой тик.
--     Используй для периодических проверок/обновлений.
--     Не делай здесь тяжёлых операций.
-- EN: Called each game tick.
--     Use for periodic checks/updates.
--     Don't do heavy operations here.
function onTick()
    tickCount = tickCount + 1

    -- RU: Логируем каждый 100-й тик.
    -- EN: Log every 100th tick.
    if tickCount % 100 == 0 then
        -- RU: Разное поведение на разных ОС.
        -- EN: Different behavior on different OSes.
        if getPlatform() == "windows" then
            logInfo("[Tick] Windows: #" .. tickCount)
        else
            logInfo("[Tick] Linux: #" .. tickCount)
        end
    end
end

-- RU: Вызывается при возникновении события.
--     eventName - строка с именем события (например "world_load", "config_change").
-- EN: Called when an event occurs.
--     eventName - event name string (e.g. "world_load", "config_change").
function onEvent(eventName)
    logInfo("Event received: " .. eventName)

    if eventName == "world_load" then
        logInfo("World loaded! Resetting tick counter.")
        tickCount = 0
    elseif eventName == "config_change" then
        local enabled = configGet("enabled", "false")
        if enabled == "true" then
            logInfo("Config: аддон включён")
        else
            logWarn("Config: аддон выключен")
        end
    end
end
