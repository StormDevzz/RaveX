package ravex.utility.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.AABB;
public class ProjectileUtility {
    public static FishingHook findOwnBobber(Minecraft mc, LocalPlayer player) {
        if (player == null || mc.level == null) return null;
        var box = AABB.ofSize(player.position(), 32, 32, 32);
        for (var e : mc.level.getEntities(player, box)) {
            if (e instanceof FishingHook hook && hook.getOwner() == player) return hook;
        }
        return null;
    }

    public static boolean hasOwnBobber(Minecraft mc, LocalPlayer player) {
        return findOwnBobber(mc, player) != null;
    }

    public static double getBobberY(Minecraft mc, LocalPlayer player) {
        var hook = findOwnBobber(mc, player);
        return hook != null ? hook.getY() : 0;
    }
}
