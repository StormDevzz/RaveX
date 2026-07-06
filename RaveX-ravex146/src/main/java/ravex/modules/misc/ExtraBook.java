package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.StringParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExtraBook extends Module {
    public static final ExtraBook INSTANCE = new ExtraBook();

    public final StringParameter newTitle = new StringParameter("Title", "RaveX Book");
    public final StringParameter newAuthor = new StringParameter("Author", "RaveX");

    private ExtraBook() {
        super("ExtraBook", Category.MISC);
        addParameter(newTitle);
        addParameter(newAuthor);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            setEnabled(false);
            return;
        }

        int slot = mc.player.getInventory().getSelectedSlot();
        var stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) {
            mc.player.displayClientMessage(
                Component.literal("§7[§cRaveX§7] §eHold a book in main hand"),
                false
            );
            setEnabled(false);
            return;
        }

        String title = newTitle.getValue();
        String author = newAuthor.getValue();
        if (title == null) title = "";
        if (author == null) author = "";

        if (stack.is(Items.WRITTEN_BOOK)) {
            WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (content != null) {
                Filterable<String> titleFilterable = title.isEmpty()
                    ? content.title()
                    : Filterable.passThrough(title.length() > 32 ? title.substring(0, 32) : title);

                String newAuthorStr = author.isEmpty() ? content.author() : author;

                WrittenBookContent modified = new WrittenBookContent(
                    titleFilterable,
                    newAuthorStr,
                    content.generation(),
                    content.pages(),
                    content.resolved()
                );
                stack.set(DataComponents.WRITTEN_BOOK_CONTENT, modified);

                mc.player.displayClientMessage(
                    Component.literal("§7[§cRaveX§7] §aBook updated: title=§f" + titleFilterable.raw()
                        + " §aauthor=§f" + newAuthorStr),
                    false
                );
            } else {
                mc.player.displayClientMessage(
                    Component.literal("§7[§cRaveX§7] §eCould not read book data"),
                    false
                );
            }
        } else if (stack.is(Items.WRITABLE_BOOK)) {
            if (title.isEmpty()) {
                mc.player.displayClientMessage(
                    Component.literal("§7[§cRaveX§7] §eProvide a title to sign the book"),
                    false
                );
                setEnabled(false);
                return;
            }

            if (mc.getConnection() == null) {
                setEnabled(false);
                return;
            }

            List<String> existingPages = new ArrayList<>();
            var writableContent = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
            if (writableContent != null) {
                for (var page : writableContent.pages()) {
                    existingPages.add(page.raw());
                }
            }

            if (title.length() > 32) title = title.substring(0, 32);
            mc.getConnection().send(new ServerboundEditBookPacket(slot, existingPages, Optional.of(title)));

            mc.player.displayClientMessage(
                Component.literal("§7[§cRaveX§7] §aBook signed with title=§f" + title),
                false
            );
        } else {
            mc.player.displayClientMessage(
                Component.literal("§7[§cRaveX§7] §eHold a writable or written book in main hand"),
                false
            );
        }

        setEnabled(false);
    }
}
