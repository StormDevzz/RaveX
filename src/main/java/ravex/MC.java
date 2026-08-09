package ravex;

import ravex.mcwrapper.GameModeWrapper;
import ravex.mcwrapper.LevelWrapper;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.mcwrapper.PlayerWrapper;

public final class MC {
    private MC() {}

    public static MinecraftWrapper mc() { return MinecraftWrapper.getWrapper(); }
    public static PlayerWrapper player() { return new PlayerWrapper(MinecraftWrapper.getWrapper().getPlayer()); }
    public static LevelWrapper level() { return new LevelWrapper(MinecraftWrapper.getWrapper().getLevel()); }
    public static GameModeWrapper gameMode() { return MinecraftWrapper.getWrapper().getGameMode(); }

    public static double x() { return MinecraftWrapper.getWrapper().getPlayerX(); }
    public static double y() { return MinecraftWrapper.getWrapper().getPlayerY(); }
    public static double z() { return MinecraftWrapper.getWrapper().getPlayerZ(); }
}
