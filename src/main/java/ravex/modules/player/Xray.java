package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;

import ravex.parameter.ActionParameter;
import ravex.utility.misc.OreUtility;
import java.util.HashSet;
import java.util.Set;
@ModuleInfo(name = "Xray", category = "net.minecraft.world.entity.player.Player")
public class Xray implements ModuleAccess {
public final ActionParameter blocks = new ActionParameter("net.minecraft.world.level.block.Blocks", () -> {
        Minecraft.getInstance().setScreen(ravex.gui.browser.BlockBrowserScreen.forXray(Minecraft.getInstance().screen));
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Xray").getEnabled();
    }
    public static Xray itz() {
        return ravex.manager.ModuleManager.delegate(Xray.class);
    }


}