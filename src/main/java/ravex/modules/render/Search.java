package ravex.modules.render;
import ravex.gui.browser.SearchBrowserScreen;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.parameter.ActionParameter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;





@Module(name = "Search", category = "Render")
public class Search {
private final Set<Identifier> selectedBlocks = new HashSet<>();
    private final Set<Identifier> selectedEntities = new HashSet<>();
    private final List<net.minecraft.core.BlockPos> foundBlocks = new ArrayList<>();

    public final ActionParameter openBrowser = new ActionParameter("Open Browser", () -> {
        var mc = MinecraftWrapper.getWrapper();
        mc.setScreen(new SearchBrowserScreen(
            mc.getCurrentScreen(),
            id -> selectedBlocks.contains(id),
            (id, sel) -> { if (sel) selectedBlocks.add(id); else selectedBlocks.remove(id); },
            id -> selectedEntities.contains(id),
            (id, sel) -> { if (sel) selectedEntities.add(id); else selectedEntities.remove(id); },
            () -> { selectedBlocks.clear(); selectedEntities.clear(); }
        ));
    });
    @Parameter(name = "Range", min = 16.0, max = 256.0, step = 8.0)
    public double range = 64.0;
    @Parameter(name = "Block Color", color = true, visible = "esp")
    public int blockColor = 0xCC00FF00;
    @Parameter(name = "Entity Color", color = true, visible = "esp")
    public int entityColor = 0xCC00FFFF;
    @Parameter(name = "ESP")
    public boolean esp = true;

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

    private long lastScanTime = 0;
    private int lastScanX = Integer.MIN_VALUE, lastScanY = 0, lastScanZ = 0;
    private boolean scanActive = false;
    private int scanMinX, scanMaxX, scanMinZ, scanMaxZ, scanMinY, scanMaxY;
    private int scanMinCx, scanMinCz, scanMaxCx, scanMaxCz, scanCurX, scanCurZ;
    private final List<net.minecraft.core.BlockPos> scanAccum = new ArrayList<>();
    private final Set<Block> scanTargets = new HashSet<>();

    private void startPass() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        net.minecraft.core.BlockPos c = mc.getPlayer().blockPosition();
        int r = (int) range;
        scanMinX = c.getX() - r;
        scanMaxX = c.getX() + r;
        scanMinZ = c.getZ() - r;
        scanMaxZ = c.getZ() + r;
        scanMinY = mc.getLevel().getMinY();
        scanMaxY = mc.getLevel().getHeight();
        scanMinCx = scanMinX >> 4;
        scanMaxCx = scanMaxX >> 4;
        scanMinCz = scanMinZ >> 4;
        scanMaxCz = scanMaxZ >> 4;
        scanCurX = scanMinCx;
        scanCurZ = scanMinCz;
        scanAccum.clear();
        scanTargets.clear();
        for (Identifier id : selectedBlocks) {
            Block b = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(id);
            if (b != null && b != Blocks.AIR) scanTargets.add(b);
        }
        lastScanTime = System.currentTimeMillis();
        lastScanX = c.getX();
        lastScanY = c.getY();
        lastScanZ = c.getZ();
        scanActive = true;
    }

    private void scanSlice() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null) {
            scanActive = false;
            return;
        }
        int done = 0;
        while (scanCurX <= scanMaxCx && done < 2) {
            scanChunk(mc, scanCurX, scanCurZ);
            scanCurZ++;
            if (scanCurZ > scanMaxCz) {
                scanCurZ = scanMinCz;
                scanCurX++;
            }
            done++;
        }
        if (scanCurX > scanMaxCx) {
            foundBlocks.clear();
            foundBlocks.addAll(scanAccum);
            scanActive = false;
        }
    }

    private void scanChunk(ravex.mcwrapper.MinecraftWrapper mc, int cx, int cz) {
        LevelChunk chunk = mc.getLevel().getChunkSource().getChunk(cx, cz, false);
        if (chunk == null) return;
        int x0 = Math.max(scanMinX, cx << 4);
        int x1 = Math.min(scanMaxX, (cx << 4) + 15);
        int z0 = Math.max(scanMinZ, cz << 4);
        int z1 = Math.min(scanMaxZ, (cz << 4) + 15);
        LevelChunkSection[] sections = chunk.getSections();
        int minSec = mc.getLevel().getMinY() >> 4;
        for (int sy = scanMinY >> 4; sy <= (scanMaxY - 1) >> 4; sy++) {
            int si = sy - minSec;
            if (si < 0 || si >= sections.length) continue;
            if (sections[si].hasOnlyAir()) continue;
            for (int bx = x0; bx <= x1; bx++) {
                for (int bz = z0; bz <= z1; bz++) {
                    for (int by = Math.max(sy << 4, scanMinY); by < Math.min((sy << 4) + 16, scanMaxY); by++) {
                        net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(bx, by, bz);
                        net.minecraft.world.level.block.state.BlockState state = chunk.getBlockState(p);
                        if (state.isAir()) continue;
                        if (scanTargets.contains(state.getBlock())) scanAccum.add(p);
                    }
                }
            }
        }
    }
    public void onTick() {
        if (Modules.enabled(Search.class)) {
            if (selectedBlocks.isEmpty() && selectedEntities.isEmpty()) {
                ravex.utility.client.ClientAlertUtility.alert(ravex.utility.misc.LanguageUtility.t("search_pick"));
                Modules.setEnabled(Search.class, false);
                return;
            }
            var mc = MinecraftWrapper.getWrapper();
            if (mc.getPlayer() == null) return;
            if (!scanActive) {
                long now = System.currentTimeMillis();
                var bp = mc.getPlayer().blockPosition();
                int dx = bp.getX() - lastScanX, dy = bp.getY() - lastScanY, dz = bp.getZ() - lastScanZ;
                if (lastScanX != Integer.MIN_VALUE
                        && now - lastScanTime < 500
                        && dx * dx + dy * dy + dz * dz < 4) return;
                startPass();
            }
            if (scanActive) scanSlice();
        }
    }
    public void onEnable() {
        if (selectedBlocks.isEmpty() && selectedEntities.isEmpty()) {
            ravex.utility.client.ClientAlertUtility.alert(ravex.utility.misc.LanguageUtility.t("search_pick"));
            Modules.setEnabled(Search.class, false);
            return;
        }
        scanActive = false;
        lastScanX = Integer.MIN_VALUE;
    }






}