package ravex.utility.player;

import net.minecraft.client.player.LocalPlayer;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class PlayerUtility {
    public static boolean isPlayerInWorld() {
        return MinecraftWrapper.getWrapper().hasPlayer() && MinecraftWrapper.getWrapper().hasWorld();
    }

    public static boolean isOverVoid() {
        var mc = MinecraftWrapper.getWrapper();
        if (!mc.hasPlayer() || !mc.hasWorld()) return false;
        var player = mc.getPlayer();
        double px = player.getX(), py = player.getY(), pz = player.getZ();
        var level = mc.getLevel();
        int minHeight = level.getMinY();
        var pos = new net.minecraft.core.BlockPos.MutableBlockPos(px, py, pz);
        for (int y = (int) py; y >= minHeight; y--) {
            pos.setY(y);
            if (!level.getBlockState(pos).isAir()) return false;
        }
        return true;
    }

    public static LocalPlayer getPlayer() {
        return MinecraftWrapper.getWrapper().getPlayer();
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
        var conn = MinecraftWrapper.getWrapper().getConnection();
        if (conn == null) return 0;
        return conn.getPlayerInfo(player.getUUID()) != null
            ? conn.getPlayerInfo(player.getUUID()).getLatency()
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

    public static boolean isOnGround(Player player) {
        return player.onGround();
    }

    public static double getFallDistance(Player player) {
        return player.fallDistance;
    }

    public static void setFallDistance(Player player, double fallDistance) {
        player.fallDistance = fallDistance;
    }

    public static int getFoodLevel(Player player) {
        return player.getFoodData().getFoodLevel();
    }

    public static Vec3 getDeltaMovement(Player player) {
        return player.getDeltaMovement();
    }

    public static void setDeltaMovement(Player player, Vec3 motion) {
        player.setDeltaMovement(motion);
    }

    public static void setDeltaMovement(Player player, double x, double y, double z) {
        player.setDeltaMovement(x, y, z);
    }
}
