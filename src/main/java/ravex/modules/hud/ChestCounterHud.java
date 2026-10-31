package ravex.modules.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Module;
import ravex.modules.Modules;
import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import ravex.modules.client.Hud;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.HudRendererUtility;

@HudModule("ChestCounterHud")
public class ChestCounterHud extends Module {

    @Parameter(name = "Range", min = 8, max = 128, step = 8)
    public double range = 48;

    @Parameter(name = "Mode", modes = {"Split", "Total"})
    public String mode = "Split";

    @Parameter(name = "ShowTotal")
    public boolean showTotal = true;

    private static final ItemStack CHEST_STACK = new ItemStack(Items.CHEST);

    private int singles = 0;
    private int doubles = 0;
    private int tickCounter = 0;

    private ChestCounterHud() {
        super("ChestCounterHud", 10, 250, 110, 24);
        setX(10);
        setY(250);
        setWidth(110);
        setHeight(24);
    }

    @Subscribe
    public void onClientTick(TickEvent.Client event) {
        if (!getEnabled()) return;
        MinecraftWrapper mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) {
            singles = 0;
            doubles = 0;
            return;
        }
        if (++tickCounter % 10 != 0) return;
        tickCounter = 0;

        Level level = mc.getLevel();
        var player = mc.getPlayer();
        double maxDistSq = range * range;
        int px = player.getBlockX() >> 4;
        int pz = player.getBlockZ() >> 4;
        int chunkRadius = (int) Math.ceil(range / 16.0) + 1;

        int countSingle = 0;
        int countDouble = 0;

        for (int cx = px - chunkRadius; cx <= px + chunkRadius; cx++) {
            for (int cz = pz - chunkRadius; cz <= pz + chunkRadius; cz++) {
                if (!level.getChunkSource().hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    double dx = player.getX() - (pos.getX() + 0.5);
                    double dy = player.getY() - (pos.getY() + 0.5);
                    double dz = player.getZ() - (pos.getZ() + 0.5);
                    if (dx * dx + dy * dy + dz * dz > maxDistSq) continue;

                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof ChestBlock) {
                        ChestType type = state.getValue(ChestBlock.TYPE);
                        if (type == ChestType.SINGLE) {
                            countSingle++;
                        } else if (type == ChestType.LEFT) {
                            countDouble++;
                        }
                    }
                }
            }
        }
        singles = countSingle;
        doubles = countDouble;
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        int bx = getX();
        int by = getY();
        int total = singles + doubles;
        int ph = 24;

        int textX = bx + 28;
        int textY = by + 8;
        int contentW;

        if (total == 0) {
            contentW = FontRenderUtility.getStringWidth("No chests nearby");
        } else if ("Total".equals(this.mode)) {
            contentW = FontRenderUtility.getStringWidth("Chests: ") + FontRenderUtility.getStringWidth(String.valueOf(total));
        } else {
            contentW = FontRenderUtility.getStringWidth("S: ") + FontRenderUtility.getStringWidth(String.valueOf(singles))
                    + 8 + FontRenderUtility.getStringWidth("D: ") + FontRenderUtility.getStringWidth(String.valueOf(doubles));
            if (showTotal) {
                contentW += 8 + FontRenderUtility.getStringWidth("|  Total: ") + FontRenderUtility.getStringWidth(String.valueOf(total));
            }
        }

        int pw = Math.max(90, textX + contentW + 8 - bx);
        setWidth(pw);
        setHeight(ph);

        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);

        graphics.renderItem(CHEST_STACK, bx + 6, by + 4);

        if (total == 0) {
            FontRenderUtility.drawString(graphics, "No chests nearby", textX, textY, 0xFF8E99A8, false);
        } else if ("Total".equals(this.mode)) {
            FontRenderUtility.drawString(graphics, "Chests: ", textX, textY, 0xFF8E99A8, false);
            int lw = FontRenderUtility.getStringWidth("Chests: ");
            FontRenderUtility.drawString(graphics, String.valueOf(total), textX + lw, textY, 0xFF38BDF8, true);
        } else {
            int curX = textX;
            FontRenderUtility.drawString(graphics, "S: ", curX, textY, 0xFF8E99A8, false);
            curX += FontRenderUtility.getStringWidth("S: ");
            FontRenderUtility.drawString(graphics, String.valueOf(singles), curX, textY, 0xFFFFFFFF, true);
            curX += FontRenderUtility.getStringWidth(String.valueOf(singles)) + 8;

            FontRenderUtility.drawString(graphics, "D: ", curX, textY, 0xFF8E99A8, false);
            curX += FontRenderUtility.getStringWidth("D: ");
            FontRenderUtility.drawString(graphics, String.valueOf(doubles), curX, textY, 0xFFFFFFFF, true);
            curX += FontRenderUtility.getStringWidth(String.valueOf(doubles));

            if (showTotal) {
                curX += 6;
                FontRenderUtility.drawString(graphics, "|", curX, textY, 0x30FFFFFF, false);
                curX += FontRenderUtility.getStringWidth("|") + 6;
                FontRenderUtility.drawString(graphics, "Total: ", curX, textY, 0xFF8E99A8, false);
                curX += FontRenderUtility.getStringWidth("Total: ");
                FontRenderUtility.drawString(graphics, String.valueOf(total), curX, textY, 0xFF38BDF8, true);
            }
        }
    }
}