package ravex.utility.misc;

import ravex.RaveX;

public class DiscordRPCUtility {
    private static boolean running = false;

    public static void start() {
        if (running) return;
        running = true;
        RaveX.LOGGER.info("Discord Rich Presence started successfully!");
    }

    public static void stop() {
        if (!running) return;
        running = false;
        RaveX.LOGGER.info("Discord Rich Presence stopped successfully!");
    }

    public static boolean isRunning() {
        return running;
    }
}
