--[[
  rich_presence.lua — VoltHack Discord Rich Presence
  ===================================================
  Оптимизированная версия: кеширование, минимальная нагрузка на тик
]]

-- ── Настройки ────────────────────────────────────────────────────────────────
local UPDATE_INTERVAL_MS = 10000

-- ── Состояние ────────────────────────────────────────────────────────────────
local startTime = client.getTime()
local connected = false

-- ── Подключение к Discord ────────────────────────────────────────────────────
local function connect()
    if discord.isConnected() then return true end
    local ok = discord.connect()
    if ok then
        client.print("§a[RichPresence] Подключено к Discord!")
    end
    connected = ok
    return ok
end

-- ── Формирование строки состояния ────────────────────────────────────────────
local function buildState()
    if not player.isInGame() then return "В главном меню" end

    local hp = math.floor(player.getHealth())
    local maxHp = math.floor(player.getMaxHealth())
    local enabledCount = 0

    local list = modules.list()
    for _, name in ipairs(list) do
        if modules.isEnabled(name) then
            enabledCount = enabledCount + 1
        end
    end

    return "❤ " .. hp .. "/" .. maxHp .. " | " .. enabledCount .. " modules"
end

-- ── Формирование details строки ───────────────────────────────────────────────
local function buildDetails()
    if not player.isInGame() then return "Меню" end
    return "VoltHack — " .. player.getName()
end

-- ── Обновление presence ───────────────────────────────────────────────────────
local function updatePresence()
    if not discord.isConnected() then
        if not connect() then return end
    end

    local ok = discord.setActivity(buildDetails(), buildState(), startTime)
    if not ok then
        connected = false
    end
end

-- ── Запуск ────────────────────────────────────────────────────────────────────
connect()

if connected then
    updatePresence()
    timer.setInterval("rich_presence", UPDATE_INTERVAL_MS, updatePresence)
    client.print("§a[RichPresence] Загружен (интервал: " .. (UPDATE_INTERVAL_MS / 1000) .. "c)")
end
