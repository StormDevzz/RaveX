--[[
  rich_presence.lua — RaveX Discord Rich Presence
  ===================================================
  Optimized version: caching, minimal tick overhead
]]

-- ── Settings ─────────────────────────────────────────────────────────────────
local UPDATE_INTERVAL_MS = 10000

-- ── State ────────────────────────────────────────────────────────────────────
local startTime = client.getTime()
local connected = false

-- ── Connecting to Discord ─────────────────────────────────────────────────────
local function connect()
    if discord.isConnected() then return true end
    local ok = discord.connect()
    if ok then
        client.print("§a[RichPresence] Connected to Discord!")
    end
    connected = ok
    return ok
end

-- ── Building State String ─────────────────────────────────────────────────────
local function buildState()
    if not player.isInGame() then return "In main menu" end

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

-- ── Building Details String ───────────────────────────────────────────────────
local function buildDetails()
    if not player.isInGame() then return "Menu" end
    return "RaveX — " .. player.getName()
end

-- ── Updating Presence ─────────────────────────────────────────────────────────
local function updatePresence()
    if not discord.isConnected() then
        if not connect() then return end
    end

    local ok = discord.setActivity(buildDetails(), buildState(), startTime)
    if not ok then
        connected = false
    end
end

-- ── Startup ───────────────────────────────────────────────────────────────────
connect()

if connected then
    updatePresence()
    timer.setInterval("rich_presence", UPDATE_INTERVAL_MS, updatePresence)
    client.print("§a[RichPresence] Loaded (interval: " .. (UPDATE_INTERVAL_MS / 1000) .. "s)")
end
