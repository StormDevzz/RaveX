package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.AABB;
import ravex.mcwrapper.MinecraftWrapper;
import org.jetbrains.annotations.Nullable;




@Module(name = "AutoFish", category = "World")
public class AutoFish {
    @Parameter(name = "CastDelay", min = 200, max = 2000, step = 100)
    public double castDelay = 600;
    @Parameter(name = "SilentSwap")
    public boolean silent = true;
    @Parameter(name = "AutoCast")
    public boolean autoCast = true;
    private long lastActionTime = 0;
    private boolean wasIdle = false;
    private double prevY = 0;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        net.minecraft.client.player.LocalPlayer player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 200) return;
        FishingHook hook = findBobber(mc, player);
        if (hook != null) {
            double dy = hook.getY() - prevY;
            boolean moving = Math.abs(dy) > 0.02;
            if (wasIdle && moving) {
                reelIn(mc, player);
                lastActionTime = now + (long) castDelay;
                wasIdle = false;
                return;
            }
            wasIdle = !moving;
            prevY = hook.getY();
            return;
        }
        wasIdle = false;
        prevY = 0;
        if (autoCast) {
            int rodSlot = findRodSlot(player);
            if (rodSlot != -1) {
                int prev = InventoryUtility.getSelectedSlot(player);
                InventoryUtility.selectSlot(player, rodSlot);
                useRod(mc, player);
                if (silent) {
                    InventoryUtility.selectSlot(player, prev);
                }
                lastActionTime = now;
            }
        }
    }
    @Nullable
    private FishingHook findBobber(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        for (var e : mc.getLevel().getEntities(player, AABB.ofSize(player.position(), 32, 32, 32))) {
            if (e instanceof FishingHook hook && hook.getOwner() == player) {
                return hook;
            }
        }
        return null;
    }
    private int findRodSlot(net.minecraft.client.player.LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.isItemInSlot(player, i, "fishing_rod")) return i;
        }
        return -1;
    }
    private void useRod(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        mc.getGameMode().useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        ravex.utility.player.SwingUtility.swingMainHand(player);
    }
    private void reelIn(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        useRod(mc, player);
    }



}