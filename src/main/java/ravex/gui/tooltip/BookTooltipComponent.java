package ravex.gui.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import ravex.utility.render.Render2DUtility;
import java.util.ArrayList;
import java.util.List;

public class BookTooltipComponent implements ClientTooltipComponent {
    private final ItemStack stack;
    private final String title;
    private final String author;
    private final int pageCount;
    private final String firstPageSnippet;

    public BookTooltipComponent(ItemStack stack) {
        this.stack = stack;
        String t = "";
        String a = "";
        int pages = 0;
        String snippet = "";

        WrittenBookContent written = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (written != null) {
            t = written.title().raw();
            a = written.author();
            pages = written.pages().size();
            if (!written.pages().isEmpty()) {
                snippet = written.pages().get(0).raw().getString();
            }
        } else {
            WritableBookContent writable = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
            if (writable != null) {
                t = "Writable Book";
                a = "You";
                pages = writable.pages().size();
                if (!writable.pages().isEmpty()) {
                    snippet = writable.pages().get(0).raw();
                }
            }
        }

        this.title = t.isEmpty() ? "Untitled" : t;
        this.author = a.isEmpty() ? "Unknown" : a;
        this.pageCount = Math.max(1, pages);
        this.firstPageSnippet = snippet;
    }

    @Override
    public int getWidth(Font font) {
        return 170;
    }

    @Override
    public int getHeight(Font font) {
        List<FormattedCharSequence> lines = getWrappedLines(font);
        int contentLines = Math.min(lines.size(), 5);
        return 28 + Math.max(1, contentLines) * 10;
    }

    private List<FormattedCharSequence> getWrappedLines(Font font) {
        if (firstPageSnippet == null || firstPageSnippet.trim().isEmpty()) {
            List<FormattedCharSequence> empty = new ArrayList<>();
            empty.add(font.split(Component.literal("§7(Empty book)"), 160).get(0));
            return empty;
        }
        return font.split(Component.literal(firstPageSnippet.replace("\r", "")), 160);
    }

    @Override
    public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics guiGraphics) {
        Render2DUtility.drawRound(guiGraphics, x - 3, y - 3, 176, height + 6, 4, 0xF014161E);
        Render2DUtility.drawRoundBorder(guiGraphics, x - 3, y - 3, 176, height + 6, 4, 1, 0xFF654321);

        guiGraphics.drawString(font, "§6§l" + font.plainSubstrByWidth(title, 110), x + 2, y + 1, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§8p. 1/" + pageCount, x + 125, y + 1, 0xFFAAAAAA, false);
        guiGraphics.drawString(font, "§7by §f" + font.plainSubstrByWidth(author, 150), x + 2, y + 12, 0xFFAAAAAA, false);

        Render2DUtility.drawRound(guiGraphics, x, y + 22, 170, 1, 0, 0x33FFFFFF);

        List<FormattedCharSequence> lines = getWrappedLines(font);
        int max = Math.min(lines.size(), 5);
        for (int i = 0; i < max; i++) {
            guiGraphics.drawString(font, lines.get(i), x + 2, y + 26 + i * 10, 0xFFCCCCCC, false);
        }
    }
}
