package ravex.gui.blockbrowser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import ravex.utility.render.FontRenderUtility;
import ravex.gui.clickgui.ColorUtility;
import ravex.gui.clickgui.ClickGUI;
import ravex.gui.clickgui.components.SearchBarWidget;
import ravex.gui.clickgui.components.TabButtonWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class BlockBrowserScreen extends Screen {
    private final Screen parent;
    private final Predicate<Identifier> isSelected;
    private final BiConsumer<Block, Boolean> onToggle;

    private static final int ITEM_SIZE = 24;
    private static final int GRID_PADDING = 5;
    private static final int HEADER_HEIGHT = 48;
    private static final int BOTTOM_BAR_HEIGHT = 30;

    private static class BlockCacheEntry {
        final Block block;
        final Identifier identifier;
        final String nameLower;
        final String idLower;
        final ItemStack stack;

        BlockCacheEntry(Block block) {
            this.block = block;
            this.identifier = BuiltInRegistries.BLOCK.getKey(block);
            this.nameLower = block.getName().getString().toLowerCase();
            this.idLower = identifier.toString().toLowerCase();
            this.stack = new ItemStack(block.asItem());
        }
    }

    private final List<BlockCacheEntry> allBlocksCache = new ArrayList<>();
    private List<BlockCacheEntry> filteredBlocksCache = new ArrayList<>();

    private final SearchBarWidget searchBar = new SearchBarWidget("Search blocks...");
    private final TabButtonWidget tabAll = new TabButtonWidget("All Blocks", 80, 16);
    private final TabButtonWidget tabSelected = new TabButtonWidget("Selected", 80, 16);
    private final TabButtonWidget tabHidden = new TabButtonWidget("Hidden", 80, 16);

    private int scrollOffset = 0;
    private int columns = 8;
    private int rows = 6;
    private long lastUpdateTime = System.currentTimeMillis();
    private float scrollAnimProgress = 0f;
    private float targetScrollAnim = 0f;
    private ItemStack hoveredItem = ItemStack.EMPTY;

    private enum Tab { ALL, SELECTED, HIDDEN }
    private Tab currentTab = Tab.ALL;

    public BlockBrowserScreen(Screen parent, Predicate<Identifier> isSelected, BiConsumer<Block, Boolean> onToggle) {
        super(Component.literal("Block Selector"));
        this.parent = parent;
        this.isSelected = isSelected;
        this.onToggle = onToggle;
    }

    @Override
    protected void init() {
        if (allBlocksCache.isEmpty()) {
            BuiltInRegistries.BLOCK.stream()
                .filter(b -> b.asItem() != null && b.asItem() != net.minecraft.world.item.Items.AIR)
                .forEach(b -> allBlocksCache.add(new BlockCacheEntry(b)));
        }
        updateFilter();
        columns = Math.max(4, (width - 40) / (ITEM_SIZE + GRID_PADDING));
        rows = Math.max(3, (height - HEADER_HEIGHT - BOTTOM_BAR_HEIGHT - 40) / (ITEM_SIZE + GRID_PADDING));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fillGradient(0, 0, this.width, this.height,
            ColorUtility.BACKGROUND_START, ColorUtility.BACKGROUND_END);

        long now = System.currentTimeMillis();
        long delta = now - lastUpdateTime;
        lastUpdateTime = now;
        if (delta > 100) delta = 16;

        searchBar.update(delta);

        int activeColor = ColorUtility.getActiveColor();

        FontRenderUtility.drawString(graphics, "Block Selector", 16, 8, activeColor, true);
        long selectedCount = allBlocksCache.stream().filter(entry -> isSelected.test(entry.identifier)).count();
        FontRenderUtility.drawString(graphics,
            selectedCount + " / " + allBlocksCache.size() + " blocks active",
            16, 22, 0xFF858599, true);

        int barW = Math.min(240, this.width - 290);
        int barX = this.width - barW - 16;
        searchBar.render(graphics, mouseX, mouseY, barX, 8, barW, 24);

        int tabX = 16;
        int tabY = HEADER_HEIGHT - 6;
        tabAll.render(graphics, mouseX, mouseY, tabX, tabY, currentTab == Tab.ALL, activeColor);
        tabSelected.render(graphics, mouseX, mouseY, tabX + 86, tabY, currentTab == Tab.SELECTED, activeColor);
        tabHidden.render(graphics, mouseX, mouseY, tabX + 172, tabY, currentTab == Tab.HIDDEN, activeColor);

        int gridX = 20;
        int gridY = HEADER_HEIGHT + 18;
        int gridWidth = columns * (ITEM_SIZE + GRID_PADDING);
        int gridHeight = rows * (ITEM_SIZE + GRID_PADDING);

        graphics.fill(gridX - 4, gridY - 4, gridX + gridWidth + 4, gridY + gridHeight + 4, 0xAA0A0A1A);
        graphics.fill(gridX - 4, gridY - 4, gridX + gridWidth + 4, gridY - 3, ColorUtility.withAlpha(activeColor, 35));
        graphics.fill(gridX - 4, gridY + gridHeight + 3, gridX + gridWidth + 4, gridY + gridHeight + 4, ColorUtility.withAlpha(activeColor, 35));

        hoveredItem = ItemStack.EMPTY;

        int totalRows = (int)Math.ceil((double)filteredBlocksCache.size() / columns);
        int maxScroll = Math.max(0, totalRows - rows);

        targetScrollAnim = scrollOffset;
        float scrollDiff = targetScrollAnim - scrollAnimProgress;
        if (Math.abs(scrollDiff) > 0.01f) {
            scrollAnimProgress += scrollDiff * 0.18f;
        } else {
            scrollAnimProgress = targetScrollAnim;
        }

        int startRow = (int)scrollAnimProgress;
        int endRow = Math.min(totalRows, startRow + rows + 1);

        graphics.enableScissor(gridX - 4, gridY - 4, gridX + gridWidth + 4, gridY + gridHeight + 4);

        for (int row = startRow; row < endRow && row < totalRows; row++) {
            for (int col = 0; col < columns; col++) {
                int index = row * columns + col;
                if (index >= filteredBlocksCache.size()) break;

                BlockCacheEntry entry = filteredBlocksCache.get(index);
                int slotX = gridX + col * (ITEM_SIZE + GRID_PADDING);
                int slotY = gridY + (row - startRow) * (ITEM_SIZE + GRID_PADDING);

                boolean hovered = mouseX >= slotX && mouseX <= slotX + ITEM_SIZE
                    && mouseY >= slotY && mouseY <= slotY + ITEM_SIZE;

                boolean selected = isSelected.test(entry.identifier);

                int slotBg = hovered ? ColorUtility.withAlpha(activeColor, 40) : (selected ? 0x2244FF44 : 0x2215152A);
                graphics.fill(slotX, slotY, slotX + ITEM_SIZE, slotY + ITEM_SIZE, slotBg);

                int borderCol = selected ? 0x8844FF44 : 0x22858599;
                if (hovered) borderCol = activeColor;

                graphics.fill(slotX, slotY, slotX + ITEM_SIZE, slotY + 1, borderCol);
                graphics.fill(slotX, slotY + ITEM_SIZE - 1, slotX + ITEM_SIZE, slotY + ITEM_SIZE, borderCol);
                graphics.fill(slotX, slotY, slotX + 1, slotY + ITEM_SIZE, borderCol);
                graphics.fill(slotX + ITEM_SIZE - 1, slotY, slotX + ITEM_SIZE, slotY + ITEM_SIZE, borderCol);

                if (hovered) {
                    hoveredItem = entry.stack;
                }

                int iconX = slotX + (ITEM_SIZE - 16) / 2;
                int iconY = slotY + (ITEM_SIZE - 16) / 2;
                graphics.renderItem(entry.stack, iconX, iconY);
            }
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            int scrollbarX = gridX + gridWidth + 8;
            int barY = gridY;
            int barHeight = gridHeight;

            graphics.fill(scrollbarX, barY, scrollbarX + 4, barY + barHeight, 0x2215152A);

            float visibleRatio = (float)rows / totalRows;
            int thumbHeight = Math.max(16, (int)(barHeight * visibleRatio));
            float scrollRatio = (float)scrollOffset / Math.max(1, maxScroll);
            int thumbY = barY + (int)((barHeight - thumbHeight) * scrollRatio);

            graphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, ColorUtility.withAlpha(activeColor, 80));
        }

        int btnW = 60;
        int btnH = 16;
        int btnX = 16;
        int btnY = this.height - 22;
        boolean backHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

        graphics.fillGradient(btnX, btnY, btnX + btnW, btnY + btnH,
            backHovered ? activeColor : 0xAA0E0E1C,
            backHovered ? ColorUtility.darker(activeColor, 0.7f) : 0x88080814
        );
        if (backHovered) {
            graphics.fill(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, activeColor);
        }
        FontRenderUtility.drawString(graphics, "Back", btnX + btnW/2 - 12, btnY + 4, 0xFFD0D0E0, true);

        if (!hoveredItem.isEmpty()) {
            renderTooltip(graphics, hoveredItem, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderTooltip(GuiGraphics graphics, ItemStack stack, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        List<Component> tooltipLines = List.of(
            stack.getHoverName(),
            Component.literal("§7" + BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
        );

        int tw = 0;
        int th = tooltipLines.size() * 10 + 6;
        for (Component line : tooltipLines) {
            tw = Math.max(tw, font.width(line) + 8);
        }

        int tx = mouseX + 12;
        int ty = mouseY - 4;
        if (tx + tw > this.width) tx = mouseX - tw - 8;
        if (ty + th > this.height) ty = mouseY - th - 4;

        int activeColor = ColorUtility.getActiveColor();
        graphics.fill(tx, ty, tx + tw, ty + th, 0xCC07070B);
        graphics.fill(tx, ty, tx + tw, ty + 1, activeColor);

        int lineY = ty + 4;
        for (Component line : tooltipLines) {
            graphics.drawString(font, line, tx + 4, lineY, 0xFFE0E0E0, false);
            lineY += 10;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int gridX = 20;
        int gridY = HEADER_HEIGHT + 18;

        int barW = Math.min(240, this.width - 290);
        int barX = this.width - barW - 16;

        if (searchBar.mouseClicked(event.x(), event.y(), barX, 8, barW, 24)) {
            return true;
        }

        int btnW = 60;
        int btnH = 16;
        int btnX = 16;
        int btnY = this.height - 22;
        if (event.x() >= btnX && event.x() <= btnX + btnW && event.y() >= btnY && event.y() <= btnY + btnH) {
            this.minecraft.setScreen(new ClickGUI());
            return true;
        }

        int tabX = 16;
        int tabY = HEADER_HEIGHT - 6;
        if (tabAll.mouseClicked(event.x(), event.y(), tabX, tabY)) {
            currentTab = Tab.ALL;
            updateFilter();
            Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.35f, 1.4f);
            return true;
        }
        if (tabSelected.mouseClicked(event.x(), event.y(), tabX + 86, tabY)) {
            currentTab = Tab.SELECTED;
            updateFilter();
            Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.35f, 1.4f);
            return true;
        }
        if (tabHidden.mouseClicked(event.x(), event.y(), tabX + 172, tabY)) {
            currentTab = Tab.HIDDEN;
            updateFilter();
            Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.35f, 1.4f);
            return true;
        }

        int col = (int)(event.x() - gridX) / (ITEM_SIZE + GRID_PADDING);
        int row = ((int)event.y() - gridY) / (ITEM_SIZE + GRID_PADDING) + (int)scrollAnimProgress;
        if (col >= 0 && col < columns && row >= 0) {
            int index = row * columns + col;
            if (index < filteredBlocksCache.size()) {
                int slotLeft = gridX + col * (ITEM_SIZE + GRID_PADDING);
                if (event.x() >= slotLeft && event.x() <= slotLeft + ITEM_SIZE) {
                    BlockCacheEntry entry = filteredBlocksCache.get(index);
                    boolean current = isSelected.test(entry.identifier);
                    onToggle.accept(entry.block, !current);
                    this.minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.4f);
                    updateFilter();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalRows = (int)Math.ceil((double)filteredBlocksCache.size() / columns);
        int maxScroll = Math.max(0, totalRows - rows);
        scrollOffset = (int)Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (searchBar.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                searchBar.setFocused(false);
                searchBar.setQuery("");
                updateFilter();
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                searchBar.keyPressed(key);
                updateFilter();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) {
                searchBar.setFocused(false);
                return true;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreen(new ClickGUI());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchBar.isFocused()) {
            String text = event.codepointAsString();
            if (!text.isEmpty()) {
                searchBar.charTyped(text.charAt(0));
                updateFilter();
            }
            return true;
        }
        return super.charTyped(event);
    }

    private void updateFilter() {
        String q = searchBar.getQuery().toLowerCase();
        List<BlockCacheEntry> baseList;

        if (currentTab == Tab.SELECTED) {
            baseList = new ArrayList<>();
            for (BlockCacheEntry entry : allBlocksCache) {
                if (isSelected.test(entry.identifier)) {
                    baseList.add(entry);
                }
            }
        } else if (currentTab == Tab.HIDDEN) {
            baseList = new ArrayList<>();
            for (BlockCacheEntry entry : allBlocksCache) {
                if (!isSelected.test(entry.identifier)) {
                    baseList.add(entry);
                }
            }
        } else {
            baseList = allBlocksCache;
        }

        if (q.isEmpty()) {
            filteredBlocksCache = new ArrayList<>(baseList);
        } else {
            filteredBlocksCache = new ArrayList<>();
            for (BlockCacheEntry entry : baseList) {
                if (entry.nameLower.contains(q) || entry.idLower.contains(q)) {
                    filteredBlocksCache.add(entry);
                }
            }
        }
        scrollOffset = 0;
    }
}
