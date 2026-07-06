

local list = modules.list()
client.print("§7[§bRaveX§7] Всего модулей: §f" .. #list)

for _, name in ipairs(list) do
    local status = modules.isEnabled(name) and "§aON" or "§cOFF"
    client.print("  " .. status .. " §7" .. name)
end

client.print("§7Активных: §a" .. modules.enabledCount())
