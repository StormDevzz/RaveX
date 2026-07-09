package ravex.modules.player;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
=======
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.world.level.block.Block;
import ravex.modules.Module;
import ravex.parameter.ActionParameter;
import ravex.utility.misc.OreUtility;
import java.util.HashSet;
import java.util.Set;
public class Xray extends Module {
<<<<<<< HEAD
=======
    public static final Xray INSTANCE = new Xray();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ActionParameter blocks = new ActionParameter("Blocks", () -> {
        Minecraft.getInstance().setScreen(ravex.gui.browser.BlockBrowserScreen.forXray(Minecraft.getInstance().screen));
    });
<<<<<<< HEAD
    private final Set<net.minecraft.resources.Identifier> selectedBlocks = new HashSet<>();
=======
    private final Set<Identifier> selectedBlocks = new HashSet<>();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

    private Xray() {
        super("Xray");
        selectedBlocks.addAll(OreUtility.getDefaultXrayBlocks());
    }
    public boolean isBlockSelected(Block block) {
        return selectedBlocks.contains(OreUtility.getIdentifier(block));
    }
    public void setBlockSelected(Block block, boolean selected) {
<<<<<<< HEAD
        var id = OreUtility.getIdentifier(block);
        if (selected) selectedBlocks.add(id);
        else selectedBlocks.remove(id);
    }
    public boolean isBlockSelected(net.minecraft.resources.Identifier id) {
        return selectedBlocks.contains(id);
    }
    public Set<net.minecraft.resources.Identifier> getSelectedBlocks() {
=======
        Identifier id = OreUtility.getIdentifier(block);
        if (selected) selectedBlocks.add(id);
        else selectedBlocks.remove(id);
    }
    public boolean isBlockSelected(Identifier id) {
        return selectedBlocks.contains(id);
    }
    public Set<Identifier> getSelectedBlocks() {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        return selectedBlocks;
    }
    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }
    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
<<<<<<< HEAD
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(Xray.class);
    }
    public static Xray itz() {
        return ModuleManager.get(Xray.class);
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }
}
