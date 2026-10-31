package ravex.mixin.network;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractClientPlayer.class)
public interface AccessorAbstractClientPlayer {
    // corpse caches PlayerInfo: unique uuid, but the victim's skin
    @Accessor("playerInfo")
    void setPlayerInfo(PlayerInfo info);
}
