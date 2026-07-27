package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;

import ravex.parameter.StringParameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "BookHelper", category = "Misc")
public class BookHelper {
    @Parameter(name = "Mode", modes = {"Edit", "Fill"})
    public String mode = "Edit";
    @Parameter(name = "Title")
    public String newTitle = "RaveXBook";
    @Parameter(name = "Author")
    public String newAuthor = "RaveX";
    @Parameter(name = "Pattern")
    public String fillPattern = "书填装模块占用空间书填装模块占用空间";
    @Parameter(name = "Pages", min = 1.0, max = 100.0, step = 1.0)
    public double maxPages = 100.0;
    @Parameter(name = "BookTitle")
    public String bookTitle = "";
    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getConnection() == null) {
            Modules.setEnabled(BookHelper.class, false);
            return;
        }
        switch (mode) {
            case "Edit" -> onEdit(mc);
            case "Fill" -> onFill(mc);
        }
        Modules.setEnabled(BookHelper.class, false);
    }

    private void onEdit(MinecraftWrapper mc) {
        int slot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        var stack = mc.getPlayer().getMainHandItem();
        if (stack.isEmpty()) {
            mc.getPlayer().displayClientMessage(
                Component.literal("§7[§cRaveX§7] §eHold a book in main hand"),
                false
            );
            return;
        }
        String title = newTitle;
        String author = newAuthor;
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
                mc.getPlayer().displayClientMessage(
                    Component.literal("§7[§cRaveX§7] §aBook updated: title=§f" + titleFilterable.raw()
                        + " §aauthor=§f" + newAuthorStr),
                    false
                );
            } else {
                mc.getPlayer().displayClientMessage(
                    Component.literal("§7[§cRaveX§7] §eCould not read book data"),
                    false
                );
            }
        } else if (InventoryUtility.isWritableBook(stack)) {
            if (title.isEmpty()) {
                mc.getPlayer().displayClientMessage(
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
            mc.getPlayer().displayClientMessage(
                Component.literal("§7[§cRaveX§7] §aBook signed with title=§f" + title),
                false
            );
        } else {
            mc.getPlayer().displayClientMessage(
                Component.literal("§7[§cRaveX§7] §eHold a writable or written book in main hand"),
                false
            );
        }
    }

    private void onFill(MinecraftWrapper mc) {
        int slot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        var stack = mc.getPlayer().getMainHandItem();
        if (!InventoryUtility.isWritableBook(stack)) {
            mc.getPlayer().displayClientMessage(
                Component.literal("§7[§cRaveX§7] §eHold a writable book in main hand"),
                false
            );
            return;
        }
        String pattern = fillPattern;
        if (pattern == null || pattern.isEmpty()) pattern = "书";
        int count = (int) maxPages;
        if (count < 1) count = 1;
        if (count > 100) count = 100;
        String fullPage = pattern.repeat(1024 / pattern.length() + 1).substring(0, 1024);
        List<String> pages = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pages.add(fullPage);
        }
        String title = bookTitle;
        if (title == null) title = "";
        if (title.isEmpty()) {
            mc.getConnection().send(new ServerboundEditBookPacket(slot, pages, Optional.empty()));
        } else {
            if (title.length() > 32) title = title.substring(0, 32);
            mc.getConnection().send(new ServerboundEditBookPacket(slot, pages, Optional.of(title)));
        }
        mc.getPlayer().displayClientMessage(
            Component.literal("§7[§cRaveX§7] §aBook filled with §f" + count + " §apages"),
            false
        );
    }




}