package ravex.gui.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.render.Render2DUtility;

public class MapTooltipComponent implements ClientTooltipComponent {
    private final ItemStack stack;

    public MapTooltipComponent(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int getHeight(Font font) {
        return 132;
    }

    @Override
    public int getWidth(Font font) {
        return 132;
    }

    @Override
    public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics guiGraphics) {
        Render2DUtility.drawRound(guiGraphics, x - 3, y - 3, 134, 134, 4, 0xEE12141C);
        Render2DUtility.drawRoundBorder(guiGraphics, x - 3, y - 3, 134, 134, 4, 1, 0xFF4A5568);

        var mc = MinecraftWrapper.getInstance();
        if (mc.level == null) return;

        MapId mapId = stack.get(DataComponents.MAP_ID);
        MapItemSavedData mapData = MapItem.getSavedData(stack, mc.level);
        if (mapData == null && mapId != null) {
            mapData = mc.level.getMapData(mapId);
        }

        if (mapData == null || mapId == null) {
            String title = mapId != null ? "Map #" + mapId.id() : "Unknown Map";
            guiGraphics.drawCenteredString(font, title, x + 64, y + 54, 0xFFAAAAAA);
            guiGraphics.drawCenteredString(font, "No Data", x + 64, y + 68, 0xFF666666);
            return;
        }

        if (mapData.colors == null) {
            guiGraphics.drawCenteredString(font, "Map #" + mapId.id(), x + 64, y + 62, 0xFFAAAAAA);
            return;
        }

        // Vanilla already maintains a 128x128 dynamic texture for every loaded map.
        // Reusing it avoids re-drawing 4k+ rectangles every frame on hover.
        try {
            Identifier texture = mc.getMapTextureManager().prepareMapTexture(mapId, mapData);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + 1, y + 1, 0f, 0f, 130, 130, 128, 128);
        } catch (Throwable ignored) {
            guiGraphics.drawCenteredString(font, "Map #" + mapId.id(), x + 64, y + 62, 0xFFAAAAAA);
        }
    }
}
