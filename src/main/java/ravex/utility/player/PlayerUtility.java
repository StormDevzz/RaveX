package ravex.utility.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.network.chat.Component;

public class PlayerUtility {
    public static boolean isPlayerInWorld() {
        return Minecraft.getInstance().player != null && Minecraft.getInstance().level != null;
    }

    public static boolean isOverVoid() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        int minHeight = mc.level.getMinY();
        var pos = new net.minecraft.core.BlockPos.MutableBlockPos(px, py, pz);
        for (int y = (int) py; y >= minHeight; y--) {
            pos.setY(y);
            if (!mc.level.getBlockState(pos).isAir()) return false;
        }
        return true;
    }

    public static LocalPlayer getPlayer() {
        return Minecraft.getInstance().player;
    }

    public static String getName(Player player) {
        return player.getName().getString();
    }

    public static String getDisplayName(Player player) {
        Component name = player.getDisplayName();
        return name != null ? name.getString() : getName(player);
    }

    public static String getTeamName(Player player) {
        PlayerTeam team = player.getTeam();
        return team != null ? team.getName() : "";
    }

    public static float getHealth(Player player) {
        return player.getHealth();
    }

    public static float getMaxHealth(Player player) {
        return player.getMaxHealth();
    }

    public static float getHealthPercent(Player player) {
        return player.getHealth() / player.getMaxHealth();
    }

    public static int getPing(Player player) {
        if (Minecraft.getInstance().getConnection() == null) return 0;
        return Minecraft.getInstance().getConnection().getPlayerInfo(player.getUUID()) != null
            ? Minecraft.getInstance().getConnection().getPlayerInfo(player.getUUID()).getLatency()
            : 0;
    }

    public static double distanceTo(Player from, Player to) {
        return from.distanceTo(to);
    }

    public static double distanceToPlayer(Player player) {
        LocalPlayer self = getPlayer();
        return self != null ? self.distanceTo(player) : Double.MAX_VALUE;
    }

    public static int getArmorValue(Player player) {
        return player.getArmorValue();
    }

    public static boolean isDead(Player player) {
        return player.isDeadOrDying();
    }

    public static boolean isCreative(Player player) {
        return player.isCreative();
    }

    public static boolean isSpectator(Player player) {
        return player.isSpectator();
    }

    public static boolean isFlying(Player player) {
        return player instanceof LocalPlayer lp && lp.getAbilities().flying;
    }

    public static boolean isSneaking(Player player) {
        return player.isShiftKeyDown();
    }

    public static boolean isSprinting(Player player) {
        return player.isSprinting();
    }

    public static boolean isUsingItem(Player player) {
        return player.isUsingItem();
    }
}
