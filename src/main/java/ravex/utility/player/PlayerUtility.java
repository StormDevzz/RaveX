package ravex.utility.player;

import net.minecraft.client.Minecraft;

public class PlayerUtility {
    public static boolean isPlayerInWorld() {
        return Minecraft.getInstance().player != null && Minecraft.getInstance().level != null;
    }
}
