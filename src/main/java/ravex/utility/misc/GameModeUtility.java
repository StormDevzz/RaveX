package ravex.utility.misc;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import ravex.mcwrapper.MinecraftWrapper;

public class GameModeUtility {
    public static GameType getGameType(Player player) {
        if (player == null) return null;
        try {
            var connection = MinecraftWrapper.getWrapper().getConnection();
            if (connection == null) return null;
            var info = connection.getPlayerInfo(player.getUUID());
            if (info == null) return null;
            return info.getGameMode();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isCreative(Player player) {
        if (player == null) return false;
        if (player == MinecraftWrapper.getWrapper().getPlayer())
            return player.getAbilities().instabuild;
        GameType type = getGameType(player);
        if (type == null) return false;
        return type == GameType.CREATIVE;
    }

    public static boolean isSurvival(Player player) {
        if (player == null) return false;
        if (player == MinecraftWrapper.getWrapper().getPlayer())
            return !player.getAbilities().instabuild && !player.isSpectator();
        GameType type = getGameType(player);
        return type == GameType.SURVIVAL || type == GameType.ADVENTURE;
    }

    public static boolean isSpectator(Player player) {
        if (player == null) return false;
        if (player.isSpectator()) return true;
        return getGameType(player) == GameType.SPECTATOR;
    }

    public static boolean isLocalCreative() {
        var player = MinecraftWrapper.getWrapper().getPlayer();
        return player != null && player.getAbilities().instabuild;
    }

    public static boolean isLocalSpectator() {
        var player = MinecraftWrapper.getWrapper().getPlayer();
        return player != null && isSpectator(player);
    }

    public static boolean isLocalAdventure() {
        var player = MinecraftWrapper.getWrapper().getPlayer();
        return player != null && getGameType(player) == GameType.ADVENTURE;
    }
}
