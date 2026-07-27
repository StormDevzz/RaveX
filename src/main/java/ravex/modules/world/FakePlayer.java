package ravex.modules.world;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.parameter.StringParameter;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.player.RemotePlayer;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "FakePlayer", category = "World")
public class FakePlayer {
    @Parameter(name = "Nickname")
    public String nickname = "FakePlayer";
    @Parameter(name = "CopyInv")
    public boolean copyInventory = true;
    private RemotePlayer remotePlayer = null;
    public void onEnable() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) {
            Modules.setEnabled(FakePlayer.class, false);
            return;
        }
        GameProfile profile = new GameProfile(UUID.fromString("c0ffeed0-dec0-4ba5-babe-0123456789ab"), nickname);
        remotePlayer = new RemotePlayer(mc.level, profile);
        remotePlayer.copyPosition(mc.player);
        remotePlayer.setYRot(mc.player.getYRot());
        remotePlayer.setXRot(mc.player.getXRot());
        remotePlayer.yHeadRot = mc.player.yHeadRot;
        remotePlayer.setId(-9999);
        if (copyInventory) {
            for (int i = 0; i < InventoryUtility.getContainerSize(mc.player); i++) {
                remotePlayer.getInventory().setItem(i, InventoryUtility.getItem(mc.player, i).copy());
            }
        }
        mc.level.addEntity(remotePlayer);
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.level != null && remotePlayer != null) {
            mc.level.removeEntity(remotePlayer.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        remotePlayer = null;
    }



}