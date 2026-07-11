package ravex.modules.world;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.StringParameter;
import ravex.parameter.BooleanParameter;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
public class FakePlayer extends Module {
    public final StringParameter nickname = new StringParameter("Nickname", "FakePlayer");
    public final BooleanParameter copyInventory = new BooleanParameter("CopyInv", true);
    private RemotePlayer remotePlayer = null;

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            setEnabled(false);
            return;
        }
        GameProfile profile = new GameProfile(UUID.fromString("c0ffeed0-dec0-4ba5-babe-0123456789ab"), nickname.getValue());
        remotePlayer = new RemotePlayer(mc.level, profile);
        remotePlayer.copyPosition(mc.player);
        remotePlayer.setYRot(mc.player.getYRot());
        remotePlayer.setXRot(mc.player.getXRot());
        remotePlayer.yHeadRot = mc.player.yHeadRot;
        remotePlayer.setId(-9999);
        if (copyInventory.getValue()) {
            for (int i = 0; i < InventoryUtility.getContainerSize(mc.player); i++) {
                remotePlayer.getInventory().setItem(i, InventoryUtility.getItem(mc.player, i).copy());
            }
        }
        mc.level.addEntity(remotePlayer);
    }
    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && remotePlayer != null) {
            mc.level.removeEntity(remotePlayer.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        remotePlayer = null;
    }
    public static FakePlayer itz() {
        return ModuleManager.get(FakePlayer.class);
    }
}
