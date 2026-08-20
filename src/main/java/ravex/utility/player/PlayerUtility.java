package ravex.utility.player;

import net.minecraft.client.player.LocalPlayer;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerUtility {
    public static boolean isPlayerInWorld() {
        var mc = MinecraftWrapper.getWrapper();
        return mc.hasPlayer() && mc.hasWorld();
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

    @Nullable
    public static LocalPlayer getPlayer() {
        return MinecraftWrapper.getWrapper().getPlayer();
    }

    @NotNull
    public static Vec3 getPosition() {
        var player = getPlayer();
        return player != null ? player.position() : Vec3.ZERO;
    }

    public static double getX() { return getPlayer() != null ? getPlayer().getX() : 0; }
    public static double getY() { return getPlayer() != null ? getPlayer().getY() : 0; }
    public static double getZ() { return getPlayer() != null ? getPlayer().getZ() : 0; }

    public static float getYaw() { return getPlayer() != null ? getPlayer().getYRot() : 0; }
    public static float getPitch() { return getPlayer() != null ? getPlayer().getXRot() : 0; }

    @NotNull
    public static Vec3 getEyePosition() {
        var player = getPlayer();
        return player != null ? player.getEyePosition(1.0F) : Vec3.ZERO;
    }

    @NotNull
    public static Vec3 getViewVector() {
        var player = getPlayer();
        return player != null ? player.getViewVector(1.0F) : Vec3.ZERO;
    }

    public static float getHealth() {
        var player = getPlayer();
        return player != null ? player.getHealth() : 0;
    }

    public static int getFoodLevel() {
        var player = getPlayer();
        return player != null ? player.getFoodData().getFoodLevel() : 20;
    }

    public static double getFallDistance() {
        var player = getPlayer();
        return player != null ? player.fallDistance : 0.0;
    }

    public static boolean isOnGround() {
        var player = getPlayer();
        return player != null && player.onGround();
    }

    public static boolean isHorizontalCollision() {
        var player = getPlayer();
        return player != null && player.horizontalCollision;
    }

    @Nullable
    public static Vec3 getDeltaMovement() {
        var player = getPlayer();
        return player != null ? player.getDeltaMovement() : null;
    }

    public static void setDeltaMovement(Vec3 motion) {
        var player = getPlayer();
        if (player != null) player.setDeltaMovement(motion);
    }

    public static void setDeltaMovement(double x, double y, double z) {
        var player = getPlayer();
        if (player != null) player.setDeltaMovement(x, y, z);
    }

    @Nullable
    public static Abilities getAbilities() {
        var player = getPlayer();
        return player != null ? player.getAbilities() : null;
    }

    @Nullable
    public static Object getInput() {
        var player = getPlayer();
        return player != null ? player.input : null;
    }

    public static Vec2 getMovementInput() {
        var player = getPlayer();
        if (player == null || player.input == null) return Vec2.ZERO;
        return player.input.getMoveVector();
    }

    @NotNull
    public static String getName(@NotNull Player player) {
        return player.getName().getString();
    }

    @NotNull
    public static String getDisplayName(@NotNull Player player) {
        Component name = player.getDisplayName();
        return name != null ? name.getString() : getName(player);
    }

    @NotNull
    public static String getTeamName(@NotNull Player player) {
        PlayerTeam team = player.getTeam();
        return team != null ? team.getName() : "";
    }

    public static float getHealth(@NotNull Player player) {
        return player.getHealth();
    }

    public static float getMaxHealth(@NotNull Player player) {
        return player.getMaxHealth();
    }

    public static float getHealthPercent(@NotNull Player player) {
        return player.getHealth() / player.getMaxHealth();
    }

    public static int getPing(@NotNull Player player) {
        var conn = MinecraftWrapper.getWrapper().getConnection();
        if (conn == null) return 0;
        return conn.getPlayerInfo(player.getUUID()) != null
            ? conn.getPlayerInfo(player.getUUID()).getLatency()
            : 0;
    }

    public static double distanceTo(@NotNull Player from, @NotNull Player to) {
        return from.distanceTo(to);
    }

    public static double distanceToPlayer(@NotNull Player player) {
        LocalPlayer self = getPlayer();
        return self != null ? self.distanceTo(player) : Double.MAX_VALUE;
    }

    public static int getArmorValue(@NotNull Player player) {
        return player.getArmorValue();
    }

    public static boolean isDead(@NotNull Player player) {
        return player.isDeadOrDying();
    }

    public static boolean isCreative(@NotNull Player player) {
        return player.isCreative();
    }

    public static boolean isSpectator(@NotNull Player player) {
        return player.isSpectator();
    }

    public static boolean isFlying(@NotNull Player player) {
        return player instanceof LocalPlayer lp && lp.getAbilities().flying;
    }

    public static boolean isSneaking(@NotNull Player player) {
        return player.isShiftKeyDown();
    }

    public static boolean isSprinting(@NotNull Player player) {
        return player.isSprinting();
    }

    public static boolean isUsingItem(@NotNull Player player) {
        return player.isUsingItem();
    }

    public static boolean isOnGround(@NotNull Player player) {
        return player.onGround();
    }

    public static double getFallDistance(@NotNull Player player) {
        return player.fallDistance;
    }

    public static void setFallDistance(@NotNull Player player, double fallDistance) {
        player.fallDistance = fallDistance;
    }

    public static int getFoodLevel(@NotNull Player player) {
        return player.getFoodData().getFoodLevel();
    }

    @NotNull
    public static Vec3 getDeltaMovement(@NotNull Player player) {
        return player.getDeltaMovement();
    }

    public static void setDeltaMovement(@NotNull Player player, @NotNull Vec3 motion) {
        player.setDeltaMovement(motion);
    }

    public static void setDeltaMovement(@NotNull Player player, double x, double y, double z) {
        player.setDeltaMovement(x, y, z);
    }
}
