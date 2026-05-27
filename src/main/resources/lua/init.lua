--[[
  ┌──────────────────────────────────────────────────────────────┐
  │  RaveX Lua Scripting API — init.lua                          │
  │  Загружается автоматически при старте LuaManager             │
  └──────────────────────────────────────────────────────────────┘
]]

-- ── Базовая библиотека ───────────────────────────────────────────

local ravex = ravex or {}

function ravex.log(msg)
    client.print("[RaveX] " .. tostring(msg))
end

function ravex.info(msg)
    client.print("§b[INFO]§7 " .. tostring(msg))
end

function ravex.success(msg)
    client.print("§a[✓]§7 " .. tostring(msg))
end

function ravex.warn(msg)
    client.print("§e[⚠]§7 " .. tostring(msg))
end

function ravex.error(msg)
    client.print("§c[✗]§7 " .. tostring(msg))
end

-- ── Утилиты ───────────────────────────────────────────────────────

function ravex.tableToString(tbl, indent)
    indent = indent or 0
    local pad = string.rep("  ", indent)
    local result = "{\n"
    for k, v in pairs(tbl) do
        local keyStr = type(k) == "string" and ('"' .. k .. '"') or tostring(k)
        if type(v) == "table" then
            result = result .. pad .. "  " .. keyStr .. " = " .. ravex.tableToString(v, indent + 1) .. ",\n"
        else
            result = result .. pad .. "  " .. keyStr .. " = " .. tostring(v) .. ",\n"
        end
    end
    result = result .. pad .. "}"
    return result
end

function ravex.round(num, decimals)
    decimals = decimals or 0
    local mult = 10 ^ decimals
    return math.floor(num * mult + 0.5) / mult
end

function ravex.clamp(val, min, max)
    return math.max(min, math.min(max, val))
end

-- ── Timer helpers ────────────────────────────────────────────────

function ravex.every(intervalMs, fn)
    local id = "ravex_timer_" .. tostring(math.random(100000, 999999))
    timer.setInterval(id, intervalMs, fn)
    return id
end

function ravex.cancel(id)
    timer.clearInterval(id)
end

-- ── Module helpers ───────────────────────────────────────────────

function ravex.getEnabledModules()
    local list = modules.list()
    local enabled = {}
    for _, name in ipairs(list) do
        if modules.isEnabled(name) then
            table.insert(enabled, name)
        end
    end
    return enabled
end

function ravex.getEnabledCount()
    local count = 0
    local list = modules.list()
    for _, name in ipairs(list) do
        if modules.isEnabled(name) then
            count = count + 1
        end
    end
    return count
end

-- ── Export ───────────────────────────────────────────────────────

ravex.version = "1.0"
ravex.NAME = "RaveX"

return ravex
