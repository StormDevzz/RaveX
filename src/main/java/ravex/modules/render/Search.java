package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.gui.browser.SearchBrowserScreen;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.parameter.ActionParameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.EntityUtility;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ravex.mcwrapper.MinecraftWrapper;





@ModuleInfo(name = "Search", category = "Render")
public class Search implements ModuleAccess {
private final Set<Identifier> selectedBlocks = new HashSet<>();
    private final Set<Identifier> selectedEntities = new HashSet<>();
    private final List<net.minecraft.core.BlockPos> foundBlocks = new ArrayList<>();

    public final ActionParameter openBrowser = new ActionParameter("Open Browser", () -> {
        var mc = MinecraftWrapper.getInstance();
        mc.setScreen(new SearchBrowserScreen(
            mc.screen,
            id -> selectedBlocks.contains(id),
            (id, sel) -> { if (sel) selectedBlocks.add(id); else selectedBlocks.remove(id); },
            id -> selectedEntities.contains(id),
            (id, sel) -> { if (sel) selectedEntities.add(id); else selectedEntities.remove(id); },
            () -> { selectedBlocks.clear(); selectedEntities.clear(); }
        ));
    });
    @Parameter(name = "Range", min = 16.0, max = 256.0, step = 8.0)
    public double range = 64.0;
    @Parameter(name = "Block Color", color = true)
    public int blockColor = 0xCC00FF00;
    @Parameter(name = "net.minecraft.world.entity.Entity Color", color = true)
    public int entityColor = 0xCC00FFFF;
    @Parameter(name = "ESP")
    public boolean esp = true;

    private Search() {
        
    }

    public boolean isBlockSelected(Identifier id) {
        return selectedBlocks.contains(id);
    }

    public boolean isEntitySelected(Identifier id) {
        return selectedEntities.contains(id);
    }

    public Set<Identifier> getSelectedBlocks() {
        return selectedBlocks;
    }

    public Set<Identifier> getSelectedEntities() {
        return selectedEntities;
    }

    public List<net.minecraft.core.BlockPos> getFoundBlocks() {
        return foundBlocks;
    }

    public void scanBlocks() {
        foundBlocks.clear();
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;

        int r = (int) range;
        net.minecraft.core.BlockPos c = mc.player.blockPosition();
        int minX = c.getX() - r, minZ = c.getZ() - r;
        int maxX = c.getX() + r, maxZ = c.getZ() + r;

        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;
                int cxStart = cx << 4, czStart = cz << 4;
                for (int bx = Math.max(minX, cxStart); bx <= Math.min(maxX, cxStart + 15); bx++) {
                    for (int bz = Math.max(minZ, czStart); bz <= Math.min(maxZ, czStart + 15); bz++) {
                        int maxY = mc.level.getHeight();
                        for (int by = mc.level.getMinY(); by < maxY; by++) {
                            net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(bx, by, bz);
                            net.minecraft.world.level.block.state.BlockState state = chunk.getBlockState(p);
                            if (state.isAir()) continue;
                            Identifier id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
                            if (selectedBlocks.contains(id)) {
                                foundBlocks.add(p);
                            }
                        }
                    }
                }
            }
        }
    }
    public void onTick() {
        if (ravex.manager.ModuleManager.INSTANCE.getByName("Search").getEnabled()) {
            scanBlocks();
        }
    }
    public void onEnable() {
        scanBlocks();
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Search").getEnabled();
    }

    public static Search itz() {
        return ravex.manager.ModuleManager.delegate(Search.class);
    }


}