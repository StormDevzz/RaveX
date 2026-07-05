package ravex.utility.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class VoidUtility {
    public static boolean isFallingIntoVoid(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        return player.getY() < mc.level.getMinY() && player.getDeltaMovement().y < 0;
    }
}
