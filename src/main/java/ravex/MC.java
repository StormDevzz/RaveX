package ravex;

import ravex.mcwrapper.GameModeWrapper;
import ravex.mcwrapper.LevelWrapper;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.mcwrapper.PlayerWrapper;

public final class MC {
    private MC() {}

    public static MinecraftWrapper mc() { return new MinecraftWrapper(); }
    public static PlayerWrapper player() { return new PlayerWrapper(net.minecraft.client.Minecraft.getInstance().player); }
    public static LevelWrapper level() { return new LevelWrapper(net.minecraft.client.Minecraft.getInstance().level); }
    public static GameModeWrapper gameMode() { return new GameModeWrapper(net.minecraft.client.Minecraft.getInstance().gameMode); }

    public static double x() { return net.minecraft.client.Minecraft.getInstance().player.getX(); }
    public static double y() { return net.minecraft.client.Minecraft.getInstance().player.getY(); }
    public static double z() { return net.minecraft.client.Minecraft.getInstance().player.getZ(); }
}
