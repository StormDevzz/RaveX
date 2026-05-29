--[[
  RaveX Lua — Discord Library
  Удобные обёртки для Discord Rich Presence
]]

local discord = {}

function discord.connect()
    return discord.connect()
end

function discord.setPresence(details, state, startTimestamp)
    return discord.setActivity(
        details or "Playing Minecraft",
        state or "",
        startTimestamp or 0
    )
end

function discord.clear()
    return discord.clearActivity()
end

function disconnect()
    discord.disconnect()
end

function discord.isReady()
    return discord.isConnected()
end

function discord.waitForConnection(timeoutMs)
    timeoutMs = timeoutMs or 5000
    local start = client.getTime()
    while not discord.isReady() do
        discord.connect()
        if client.getTime() - start > timeoutMs then
            return false
        end
    end
    return true
end

return discord
