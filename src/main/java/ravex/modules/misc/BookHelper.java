package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.StringParameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ravex.utility.player.InventoryUtility;
public class BookHelper extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Edit", List.of("Edit", "Fill"));
    public final StringParameter newTitle = new StringParameter("Title", "RaveXBook");
    public final StringParameter newAuthor = new StringParameter("Author", "RaveX");
    public final StringParameter fillPattern = new StringParameter("Pattern", "书填装模块占用空间书填装模块占用空间");
    public final NumberParameter maxPages = new NumberParameter("Pages", 100.0, 1.0, 100.0, 1.0);
    public final StringParameter bookTitle = new StringParameter("BookTitle", "");

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            setEnabled(false);
            return;
        }
        switch (mode.getValue()) {
            case "Edit" -> onEdit(mc);
            case "Fill" -> onFill(mc);
        }
        setEnabled(false);
    }

    private void onEdit(Minecraft mc) {
        int slot = InventoryUtility.getSelectedSlot(mc.player);
        var stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) {
            mc.player.displayClientMessage(
                Component.literal("§7[§cRaveX§7] §eHold a book in main hand"),
                false
            );
            return;
        }
        String title = newTitle.getValue();
        String author = newAuthor.getValue();
        if (title == null) title = "";
        if (author == null) author = "";
        if (InventoryUtility.isWrittenBook(stack)) {
            WrittenBookContent content = InventoryUtility.getWrittenBookContent(stack);
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
                InventoryUtility.setWrittenBookContent(stack, modified);
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
        } else if (InventoryUtility.isWritableBook(stack)) {
            if (title.isEmpty()) {
                mc.player.displayClientMessage(
                    Component.literal("§7[§cRaveX§7] §eProvide a title to sign the book"),
                    false
                );
                return;
            }
            List<String> existingPages = new ArrayList<>();
            var writableContent = InventoryUtility.getWritableBookContent(stack);
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
    }

    private void onFill(Minecraft mc) {
        int slot = InventoryUtility.getSelectedSlot(mc.player);
        var stack = mc.player.getMainHandItem();
        if (!InventoryUtility.isWritableBook(stack)) {
            mc.player.displayClientMessage(
                Component.literal("§7[§cRaveX§7] §eHold a writable book in main hand"),
                false
            );
            return;
        }
        String pattern = fillPattern.getValue();
        if (pattern == null || pattern.isEmpty()) pattern = "书";
        int count = maxPages.getValue().intValue();
        if (count < 1) count = 1;
        if (count > 100) count = 100;
        String fullPage = pattern.repeat(1024 / pattern.length() + 1).substring(0, 1024);
        List<String> pages = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pages.add(fullPage);
        }
        String title = bookTitle.getValue();
        if (title == null) title = "";
        if (title.isEmpty()) {
            mc.getConnection().send(new ServerboundEditBookPacket(slot, pages, Optional.empty()));
        } else {
            if (title.length() > 32) title = title.substring(0, 32);
            mc.getConnection().send(new ServerboundEditBookPacket(slot, pages, Optional.of(title)));
        }
        mc.player.displayClientMessage(
            Component.literal("§7[§cRaveX§7] §aBook filled with §f" + count + " §apages"),
            false
        );
    }

    public static BookHelper itz() {
        return ModuleManager.get(BookHelper.class);
    }
}
