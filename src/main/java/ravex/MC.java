package ravex;

import ravex.mcwrapper.GameModeWrapper;
import ravex.mcwrapper.LevelWrapper;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.mcwrapper.PlayerWrapper;

public final class MC {
    private MC() {}

    public static MinecraftWrapper mc() { return new MinecraftWrapper(); }
    public static PlayerWrapper player() { return new PlayerWrapper(MinecraftWrapper.getInstance().player); }
    public static LevelWrapper level() { return new LevelWrapper(MinecraftWrapper.getInstance().level); }
    public static GameModeWrapper gameMode() { return new GameModeWrapper(MinecraftWrapper.getInstance().gameMode); }

    public static double x() { return MinecraftWrapper.getInstance().player.getX(); }
    public static double y() { return MinecraftWrapper.getInstance().player.getY(); }
    public static double z() { return MinecraftWrapper.getInstance().player.getZ(); }
}
