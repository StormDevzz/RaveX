package ravex.utility.misc;
import net.minecraft.client.Minecraft;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.AABB;
public class ProjectileUtility {
    public static FishingHook findOwnBobber(MinecraftWrapper mc, LocalPlayer player) {
        var _mc = mc.getRaw();
        if (player == null || _mc.level == null) return null;
        var box = AABB.ofSize(player.position(), 32, 32, 32);
        for (var e : _mc.level.getEntities(player, box)) {
            if (e instanceof FishingHook hook && hook.getOwner() == player) return hook;
        }
        return null;
    }

    public static boolean hasOwnBobber(MinecraftWrapper mc, LocalPlayer player) {
        var _mc = mc.getRaw();
        return findOwnBobber(mc, player) != null;
    }

    public static double getBobberY(MinecraftWrapper mc, LocalPlayer player) {
        var _mc = mc.getRaw();
        var hook = findOwnBobber(mc, player);
        return hook != null ? hook.getY() : 0;
    }
}
