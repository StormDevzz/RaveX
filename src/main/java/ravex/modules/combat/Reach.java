package ravex.modules.combat;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
public class Reach extends Module {
    public final NumberParameter entityRange = new NumberParameter("EntityReach", 4.5, 3.0, 6.0, 0.1);
    public final NumberParameter blockRange = new NumberParameter("BlockReach", 5.5, 4.5, 7.0, 0.1);

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var entityAttr = mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (entityAttr != null) {
            entityAttr.setBaseValue(entityRange.getValue());
        }
        var blockAttr = mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (blockAttr != null) {
            blockAttr.setBaseValue(blockRange.getValue());
        }
    }
    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var entityAttr = mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (entityAttr != null) {
            entityAttr.setBaseValue(3.0); 
        }
        var blockAttr = mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (blockAttr != null) {
            blockAttr.setBaseValue(4.5); 
        }
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(Reach.class);
    }
    public static Reach itz() {
        return ModuleManager.get(Reach.class);
    }

}