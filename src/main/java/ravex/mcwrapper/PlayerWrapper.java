package ravex.mcwrapper;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class PlayerWrapper {
    private final LocalPlayer player;

    public PlayerWrapper(LocalPlayer player) {
        this.player = player;
    }

    public LocalPlayer getRaw() { return player; }

    public String getName() {
        return player.getName().getString();
    }

    public Vec3 getPosition() {
        return player.position();
    }

    public double getX() { return player.getX(); }
    public double getY() { return player.getY(); }
    public double getZ() { return player.getZ(); }

    public float getYaw() { return player.getYRot(); }
    public float getPitch() { return player.getXRot(); }

    public float getHealth() { return player.getHealth(); }
    public float getMaxHealth() { return player.getMaxHealth(); }
    public float getAbsorption() { return player.getAbsorptionAmount(); }
    public int getFoodLevel() { return player.getFoodData().getFoodLevel(); }
    public float getSaturation() { return player.getFoodData().getSaturationLevel(); }

    public int getAirSupply() { return player.getAirSupply(); }
    public boolean isUnderWater() { return player.isUnderWater(); }
    public boolean isOnGround() { return player.onGround(); }
    public boolean isSprinting() { return player.isSprinting(); }
    public boolean isSneaking() { return player.isShiftKeyDown(); }

    public double getFallDistance() { return player.fallDistance; }

    public ItemStack getMainHand() { return player.getMainHandItem(); }
    public ItemStack getOffHand() { return player.getOffhandItem(); }

    public ItemStack getHelmet() { return player.getItemBySlot(EquipmentSlot.HEAD); }
    public ItemStack getChestplate() { return player.getItemBySlot(EquipmentSlot.CHEST); }
    public ItemStack getLeggings() { return player.getItemBySlot(EquipmentSlot.LEGS); }
    public ItemStack getBoots() { return player.getItemBySlot(EquipmentSlot.FEET); }

    public Inventory getInventory() { return player.getInventory(); }

    public int getSelectedSlot() { return player.getInventory().getSelectedSlot(); }
    public ItemStack getSelectedItem() {
        return player.getInventory().getSelectedSlot() >= 0
            ? player.getInventory().getItem(player.getInventory().getSelectedSlot())
            : ItemStack.EMPTY;
    }

    public boolean isUsingItem() { return player.isUsingItem(); }
    public ItemStack getUsingItem() { return player.getUseItem(); }

    public float getSwingProgress(float partialTick) {
        return player.getAttackAnim(partialTick);
    }

    public int getHurtTime() { return player.hurtTime; }
    public int getInvulnerableTime() { return player.invulnerableTime; }

    public void sendChatMessage(String message) {
        player.connection.sendChat(message);
    }

    public void displayClientMessage(net.minecraft.network.chat.Component component, boolean actionBar) {
        player.displayClientMessage(component, actionBar);
    }

    public void swing() { player.swing(net.minecraft.world.InteractionHand.MAIN_HAND); }

    public void setYaw(float yaw) { player.setYRot(yaw); }
    public void setPitch(float pitch) { player.setXRot(pitch); }

    public Vec3 getVelocity() { return player.getDeltaMovement(); }
    public void setVelocity(Vec3 velocity) { player.setDeltaMovement(velocity); }

    public Object getInput() { return player.input; }
}
