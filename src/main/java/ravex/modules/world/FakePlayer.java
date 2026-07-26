package ravex.modules.world;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.StringParameter;
import ravex.parameter.BooleanParameter;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
@ModuleInfo(name = "FakePlayer", category = "World")
public class FakePlayer extends ravex.modules.Module {
public final StringParameter nickname = new StringParameter("Nickname", "FakePlayer");
    public final BooleanParameter copyInventory = new BooleanParameter("CopyInv", true);
    private RemotePlayer remotePlayer = null;
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            enabled = false;
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
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && remotePlayer != null) {
            mc.level.removeEntity(remotePlayer.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        remotePlayer = null;
    }
    public static FakePlayer itz() {
        return ravex.manager.ModuleManager.delegate(FakePlayer.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}