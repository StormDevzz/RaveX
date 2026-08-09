package ravex.mcwrapper;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.inventory.ClickType;

public class GameModeWrapper {
    private final MultiPlayerGameMode gameMode;

    public GameModeWrapper(MultiPlayerGameMode gameMode) {
        this.gameMode = gameMode;
    }

    public MultiPlayerGameMode getRaw() { return gameMode; }

    public boolean attack(LocalPlayer player, Entity target) {
        if (gameMode == null) return false;
        gameMode.attack(player, target);
        return true;
    }

    public boolean interact(LocalPlayer player, Entity target, InteractionHand hand) {
        if (gameMode == null) return false;
        gameMode.interact(player, target, hand);
        return true;
    }

    public boolean useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        if (gameMode == null) return false;
        gameMode.useItemOn(player, hand, hit);
        return true;
    }

    public boolean useItem(LocalPlayer player, InteractionHand hand) {
        if (gameMode == null) return false;
        gameMode.useItem(player, hand);
        return true;
    }

    public boolean startDestroyBlock(BlockPos pos, Direction face) {
        if (gameMode == null) return false;
        gameMode.startDestroyBlock(pos, face);
        return true;
    }

    public boolean continueDestroyBlock(BlockPos pos, Direction face) {
        if (gameMode == null) return false;
        gameMode.continueDestroyBlock(pos, face);
        return true;
    }

    public void stopDestroyBlock() {
        if (gameMode != null) gameMode.stopDestroyBlock();
    }

    public boolean isDestroying() {
        return gameMode != null && gameMode.isDestroying();
    }

    public boolean handleInventoryMouseClick(int containerId, int slot, int button, ClickType clickType, Player player) {
        if (gameMode == null) return false;
        gameMode.handleInventoryMouseClick(containerId, slot, button, clickType, player);
        return true;
    }

    public void releaseUsingItem(Player player) {
        if (gameMode != null) gameMode.releaseUsingItem(player);
    }
}
