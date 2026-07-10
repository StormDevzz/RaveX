package ravex.modules.render;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import ravex.gui.browser.SearchBrowserScreen;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ActionParameter;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Search extends Module {
    private final Set<Identifier> selectedBlocks = new HashSet<>();
    private final Set<Identifier> selectedEntities = new HashSet<>();
    private final List<BlockPos> foundBlocks = new ArrayList<>();

    public final ActionParameter openBrowser = new ActionParameter("Open Browser", () -> {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new SearchBrowserScreen(
            mc.screen,
            id -> selectedBlocks.contains(id),
            (id, sel) -> { if (sel) selectedBlocks.add(id); else selectedBlocks.remove(id); },
            id -> selectedEntities.contains(id),
            (id, sel) -> { if (sel) selectedEntities.add(id); else selectedEntities.remove(id); },
            () -> { selectedBlocks.clear(); selectedEntities.clear(); }
        ));
    });
    public final NumberParameter range = new NumberParameter("Range", 64.0, 16.0, 256.0, 8.0);
    public final ColorParameter blockColor = new ColorParameter("Block Color", 0xCC00FF00);
    public final ColorParameter entityColor = new ColorParameter("Entity Color", 0xCC00FFFF);
    public final BooleanParameter esp = new BooleanParameter("ESP", true);

    private Search() {
        super("Search");
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

    public List<BlockPos> getFoundBlocks() {
        return foundBlocks;
    }

    public void scanBlocks() {
        foundBlocks.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int r = range.getValue().intValue();
        BlockPos c = mc.player.blockPosition();
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
                            BlockPos p = new BlockPos(bx, by, bz);
                            BlockState state = chunk.getBlockState(p);
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

    @Override
    public void onTick() {
        if (getEnabled()) {
            scanBlocks();
        }
    }

    @Override
    public void onEnable() {
        scanBlocks();
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(Search.class);
    }

    public static Search itz() {
        return ModuleManager.get(Search.class);
    }
}
