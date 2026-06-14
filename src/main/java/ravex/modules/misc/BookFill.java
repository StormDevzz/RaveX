package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.world.item.Items;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.parameter.StringParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookFill extends Module {
    public static final BookFill INSTANCE = new BookFill();

    public final StringParameter fillPattern = new StringParameter("Pattern", "书填装模块占用空间书填装模块占用空间");
    public final NumberParameter maxPages = new NumberParameter("Pages", 100.0, 1.0, 100.0, 1.0);
    public final StringParameter bookTitle = new StringParameter("Title", "");

    private BookFill() {
        super("BookFill", Category.MISC);
        addParameter(fillPattern);
        addParameter(maxPages);
        addParameter(bookTitle);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            setEnabled(false);
            return;
        }

        int slot = mc.player.getInventory().getSelectedSlot();
        var stack = mc.player.getMainHandItem();
        if (!stack.is(Items.WRITABLE_BOOK)) {
            mc.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§7[§cRaveX§7] §eHold a writable book in main hand"),
                false
            );
            setEnabled(false);
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
            net.minecraft.network.chat.Component.literal("§7[§cRaveX§7] §aBook filled with §f" + count + " §apages"),
            false
        );

        setEnabled(false);
    }
}
