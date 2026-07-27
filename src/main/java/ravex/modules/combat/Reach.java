package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.world.entity.ai.attributes.Attributes;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "Reach", category = "Combat")
public class Reach implements ModuleAccess {
    @Parameter(name = "EntityReach", min = 3.0, max = 6.0, step = 0.1)
    public double entityRange = 4.5;
    @Parameter(name = "BlockReach", min = 4.5, max = 7.0, step = 0.1)
    public double blockRange = 5.5;
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;
        var entityAttr = mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (entityAttr != null) {
            entityAttr.setBaseValue(entityRange);
        }
        var blockAttr = mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (blockAttr != null) {
            blockAttr.setBaseValue(blockRange);
        }
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getInstance();
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("Reach").getEnabled();
    }
    public static Reach itz() {
        return ravex.manager.ModuleManager.delegate(Reach.class);
    }


}