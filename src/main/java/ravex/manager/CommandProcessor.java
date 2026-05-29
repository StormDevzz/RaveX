package ravex.manager;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ravex.modules.misc.Commands;

import java.util.List;

public class CommandProcessor {
    public static final CommandProcessor INSTANCE = new CommandProcessor();

    private CommandProcessor() {}

    /**
     * Intercepts chat and processes client-side commands.
     * @return true if the message was processed as a command and should be cancelled.
     */
    public boolean processCommand(String message) {
        if (!Commands.INSTANCE.getEnabled()) {
            return false;
        }

        String pref = Commands.INSTANCE.prefix.getValue();
        if (message.startsWith(pref)) {
            String rawCmd = message.substring(pref.length()).trim();
            if (rawCmd.isEmpty()) return true;

            String[] args = rawCmd.split("\\s+");
            String commandName = args[0].toLowerCase();

            if (commandName.equals("cfg") || commandName.equals("config")) {
                handleConfigCommand(args);
                return true;
            }

            // Unknown command fallback
            printMessage("§c[RaveX] Unknown command. Type §e" + pref + "config §cto manage client configurations.");
            return true;
        }
        return false;
    }

    private void handleConfigCommand(String[] args) {
        if (args.length < 2) {
            printHelp();
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "save":
                if (args.length < 3) {
                    printMessage("§c[RaveX] Usage: .config save <name>");
                } else {
                    String name = args[2];
                    if (ConfigManager.INSTANCE.save(name)) {
                        printMessage("§a[RaveX] Successfully saved config: §e" + name);
                    } else {
                        printMessage("§c[RaveX] Failed to save config: §e" + name);
                    }
                }
                break;

            case "load":
                if (args.length < 3) {
                    printMessage("§c[RaveX] Usage: .config load <name>");
                } else {
                    String name = args[2];
                    if (ConfigManager.INSTANCE.load(name)) {
                        printMessage("§a[RaveX] Successfully loaded config: §e" + name);
                    } else {
                        printMessage("§c[RaveX] Failed to load config (does it exist?): §e" + name);
                    }
                }
                break;

            case "list":
                List<String> configs = ConfigManager.INSTANCE.list();
                if (configs.isEmpty()) {
                    printMessage("§e[RaveX] No configurations found.");
                } else {
                    printMessage("§a[RaveX] Available configurations:");
                    for (String cfg : configs) {
                        printMessage("§7 - §e" + cfg);
                    }
                }
                break;

            case "delete":
                if (args.length < 3) {
                    printMessage("§c[RaveX] Usage: .config delete <name>");
                } else {
                    String name = args[2];
                    if (ConfigManager.INSTANCE.delete(name)) {
                        printMessage("§a[RaveX] Successfully deleted config: §e" + name);
                    } else {
                        printMessage("§c[RaveX] Failed to delete config: §e" + name);
                    }
                }
                break;

            default:
                printHelp();
                break;
        }
    }

    private void printHelp() {
        String pref = Commands.INSTANCE.prefix.getValue();
        printMessage("§5[RaveX] Config Commands Help:");
        printMessage(" §e" + pref + "config save <name>  §7- Saves current settings");
        printMessage(" §e" + pref + "config load <name>  §7- Loads settings from file");
        printMessage(" §e" + pref + "config list         §7- Lists all configurations");
        printMessage(" §e" + pref + "config delete <name>§7- Deletes a configuration");
    }

    private void printMessage(String text) {
        if (!Commands.INSTANCE.showFeedback.getValue()) {
            // Suppress non-warning and non-error info logs if feedback is disabled
            if (!text.contains("§c") && !text.contains("§5") && !text.contains("Help")) {
                return;
            }
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(text), false);
        }
    }
}
