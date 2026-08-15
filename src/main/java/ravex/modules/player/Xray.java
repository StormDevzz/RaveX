package ravex.modules.player;
import ravex.modules.annotations.Module;
import net.minecraft.world.level.block.Block;

import ravex.parameter.ActionParameter;
import ravex.utility.misc.OreUtility;
import java.util.HashSet;
import java.util.Set;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "Xray", category = "Player")
public class Xray {
    public final ActionParameter blocks = new ActionParameter("Blocks", () -> {
        MinecraftWrapper.getWrapper().setScreen(ravex.gui.browser.BlockBrowserScreen.forXray(MinecraftWrapper.getWrapper().getCurrentScreen()));
    });
    private final Set<net.minecraft.resources.Identifier> selectedBlocks = new HashSet<>();

    private Xray() {
        
        selectedBlocks.addAll(OreUtility.getDefaultXrayBlocks());
    }
    public boolean isBlockSelected(Block block) {
        return selectedBlocks.contains(OreUtility.getIdentifier(block));
    }
    public void setBlockSelected(Block block, boolean selected) {
        var id = OreUtility.getIdentifier(block);
        if (selected) selectedBlocks.add(id);
        else selectedBlocks.remove(id);
    }
    public boolean isBlockSelected(net.minecraft.resources.Identifier id) {
        return selectedBlocks.contains(id);
    }
    public Set<net.minecraft.resources.Identifier> getSelectedBlocks() {
        return selectedBlocks;
    }
    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevelRenderer() != null) mc.getLevelRenderer().allChanged();
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevelRenderer() != null) mc.getLevelRenderer().allChanged();
    }




}