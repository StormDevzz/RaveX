package ravex.modules.world;
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
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) {
            Modules.setEnabled(FakePlayer.class, false);
            return;
        }
        GameProfile profile = new GameProfile(UUID.fromString("c0ffeed0-dec0-4ba5-babe-0123456789ab"), nickname);
        remotePlayer = new RemotePlayer(mc.getLevel(), profile);
        remotePlayer.copyPosition(mc.getPlayer());
        remotePlayer.setYRot(mc.getPlayer().getYRot());
        remotePlayer.setXRot(mc.getPlayer().getXRot());
        remotePlayer.yHeadRot = mc.getPlayer().yHeadRot;
        remotePlayer.setId(-9999);
        if (copyInventory) {
            for (int i = 0; i < InventoryUtility.getContainerSize(mc.getPlayer()); i++) {
                remotePlayer.getInventory().setItem(i, InventoryUtility.getItem(mc.getPlayer(), i).copy());
            }
        }
        mc.getLevel().addEntity(remotePlayer);
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() != null && remotePlayer != null) {
            mc.getLevel().removeEntity(remotePlayer.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        remotePlayer = null;
    }



}