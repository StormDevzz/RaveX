--[[
  RaveX Lua — Player Library
  Расширенные функции для работы с игроком
]]

local player = {}

function player.getHunger()
    return 20
end

function player.getHealthPercent()
    if not player.isInGame() then return 0 end
    local hp = player.getHealth()
    local max = player.getMaxHealth()
    if max <= 0 then return 0 end
    return (hp / max) * 100
end

function player.getHealthBar()
    local pct = player.getHealthPercent()
    local bars = math.floor(pct / 10)
    local result = ""
    for i = 1, 10 do
        if i <= bars then
            result = result .. "§a█"
        else
            result = result .. "§c█"
        end
    end
    return result
end

function player.getCoords()
    if not player.isInGame() then return "0, 0, 0" end
    local x = math.floor(player.getX())
    local y = math.floor(player.getY())
    local z = math.floor(player.getZ())
    return x .. ", " .. y .. ", " .. z
end

function player.getDirection()
    return 0
end

return player
