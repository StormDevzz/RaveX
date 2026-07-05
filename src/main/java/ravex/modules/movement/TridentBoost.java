package ravex.modules.movement;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
public class TridentBoost extends Module {
    public static final TridentBoost INSTANCE = new TridentBoost();
    public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "Always"));
    public final NumberParameter speed = new NumberParameter("Speed", 1.0, 0.5, 3.0, 0.1);
    public final NumberParameter vertical = new NumberParameter("Vertical", 0.5, 0.0, 2.0, 0.1);

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack main = mc.player.getMainHandItem();
        if (main.getItem() != Items.TRIDENT) return;
        if (!hasRiptide(main)) return;
        if (!mc.player.isUsingItem()) return;
        String m = mode.getValue();
        if (m.equals("Normal") && !mc.player.isInWaterOrRain()) return;
        float yaw = mc.player.getYRot() * ((float)Math.PI / 180F);
        float pitch = mc.player.getXRot() * ((float)Math.PI / 180F);
        double mult = speed.getValue();
        double vert = vertical.getValue();
        double dx = -Math.sin(yaw) * Math.cos(pitch) * mult;
        double dy = -Math.sin(pitch) * vert;
        double dz = Math.cos(yaw) * Math.cos(pitch) * mult;
        mc.player.setDeltaMovement(new Vec3(dx, dy, dz));
        mc.player.hurtMarked = true;
    }
    private boolean hasRiptide(ItemStack stack) {
        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : enchants.keySet()) {
            String id = holder.getRegisteredName();
            if (id != null && id.contains("riptide")) return true;
        }
        return false;
    }
}
