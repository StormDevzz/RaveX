package ravex.modules.world;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class AutoFish extends Module {
    public static final AutoFish INSTANCE = new AutoFish();
    public final NumberParameter castDelay = new NumberParameter("CastDelay(ms)", 600, 200, 2000, 100);
    public final BooleanParameter silent = new BooleanParameter("SilentSwap", true);
    public final BooleanParameter autoCast = new BooleanParameter("AutoCast", true);
    private long lastActionTime = 0;
    private boolean wasIdle = false;
    private double prevY = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 200) return;
        FishingHook hook = findBobber(mc, player);
        if (hook != null) {
            double dy = hook.getY() - prevY;
            boolean moving = Math.abs(dy) > 0.02;
            if (wasIdle && moving) {
                reelIn(mc, player);
                lastActionTime = now + castDelay.getValue().longValue();
                wasIdle = false;
                return;
            }
            wasIdle = !moving;
            prevY = hook.getY();
            return;
        }
        wasIdle = false;
        prevY = 0;
        if (autoCast.getValue()) {
            int rodSlot = findRodSlot(player);
            if (rodSlot != -1) {
                int prev = player.getInventory().getSelectedSlot();
                player.getInventory().setSelectedSlot(rodSlot);
                useRod(mc, player);
                if (silent.getValue()) {
                    player.getInventory().setSelectedSlot(prev);
                }
                lastActionTime = now;
            }
        }
    }
    private FishingHook findBobber(Minecraft mc, LocalPlayer player) {
        for (Entity e : mc.level.getEntities(player, AABB.ofSize(player.position(), 32, 32, 32))) {
            if (e instanceof FishingHook hook && hook.getOwner() == player) {
                return hook;
            }
        }
        return null;
    }
    private int findRodSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.FISHING_ROD)) return i;
        }
        return -1;
    }
    private void useRod(Minecraft mc, LocalPlayer player) {
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        player.swing(InteractionHand.MAIN_HAND);
    }
    private void reelIn(Minecraft mc, LocalPlayer player) {
        useRod(mc, player);
    }
}
