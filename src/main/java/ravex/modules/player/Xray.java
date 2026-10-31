package ravex.modules.player;
import ravex.modules.annotations.Module;

import ravex.parameter.ActionParameter;
import ravex.utility.misc.block.BlockUtility;
import java.util.HashSet;
import java.util.Set;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "Xray", category = "Misc")
public class Xray {
    public final ActionParameter blocks = new ActionParameter("Blocks", () -> {
        MinecraftWrapper.getWrapper().setScreen(ravex.gui.browser.BlockBrowserScreen.forXray(MinecraftWrapper.getWrapper().getCurrentScreen()));
    });
    private final Set<net.minecraft.resources.Identifier> selectedBlocks = new HashSet<>();

    private Xray() {
        
        selectedBlocks.addAll(BlockUtility.getDefaultXrayBlocks());
    }
    public boolean isBlockSelected(net.minecraft.world.level.block.Block block) {
        return selectedBlocks.contains(BlockUtility.getIdentifier(block));
    }
    public void setBlockSelected(net.minecraft.world.level.block.Block block, boolean selected) {
        var id = BlockUtility.getIdentifier(block);
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