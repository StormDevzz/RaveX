package ravex.gui.book;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import ravex.utility.misc.LanguageUtility;
import ravex.utility.network.NetworkUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// edits the held book locally, then sends an edit packet
public class EditWrittenBookScreen extends Screen {
    private final ItemStack book;
    private final int slot;
    private EditBox titleBox;
    private EditBox authorBox;

    public EditWrittenBookScreen(ItemStack book, int slot) {
        super(Component.literal(LanguageUtility.t("book_edit_button")));
        this.book = book;
        this.slot = slot;
    }

    @Override
    protected void init() {
        WrittenBookContent content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        String title = content != null ? content.title().raw() : "";
        String author = content != null ? content.author() : "";
        int left = this.width / 2 - 100;
        titleBox = new EditBox(this.font, left, 62, 200, 20, Component.literal(LanguageUtility.t("book_title")));
        titleBox.setMaxLength(32);
        titleBox.setValue(title);
        authorBox = new EditBox(this.font, left, 104, 200, 20, Component.literal(LanguageUtility.t("book_author")));
        authorBox.setMaxLength(64);
        authorBox.setValue(author);
        addRenderableWidget(titleBox);
        addRenderableWidget(authorBox);
        addRenderableWidget(Button.builder(Component.literal(LanguageUtility.t("book_done")), b -> apply())
            .bounds(left, 140, 200, 20).build());
    }

    private void apply() {
        WrittenBookContent content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null) {
            onClose();
            return;
        }
        String title = titleBox.getValue();
        if (title.length() > 32) title = title.substring(0, 32);
        String author = authorBox.getValue();
        WrittenBookContent modified = new WrittenBookContent(
            Filterable.passThrough(title),
            author,
            content.generation(),
            content.pages(),
            content.resolved()
        );
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, modified);

        List<String> pages = new ArrayList<>();
        var access = new net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess(modified.getPages(false));
        for (int i = 0; i < access.getPageCount(); i++) {
            pages.add(access.getPage(i).getString());
        }
        NetworkUtility.sendPacket(new ServerboundEditBookPacket(slot, pages, Optional.of(title)));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(0, 0, this.width, this.height, 0xB0101018);
        super.render(graphics, mouseX, mouseY, partialTicks);
        int left = this.width / 2 - 100;
        graphics.drawString(this.font, LanguageUtility.t("book_title"), left, 50, 0xFFB0B0C0);
        graphics.drawString(this.font, LanguageUtility.t("book_author"), left, 92, 0xFFB0B0C0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
