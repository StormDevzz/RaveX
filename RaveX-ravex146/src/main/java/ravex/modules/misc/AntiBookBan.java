package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;

public class AntiBookBan extends Module {
    public static final AntiBookBan INSTANCE = new AntiBookBan();

    public final BooleanParameter blockAll = new BooleanParameter("Block All", false);
    public final NumberParameter maxPages = new NumberParameter("Max Pages", 100.0, 10.0, 200.0, 1.0);
    public final NumberParameter maxTotalChars = new NumberParameter("Max Chars", 50000.0, 5000.0, 500000.0, 1000.0);

    private AntiBookBan() {
        super("AntiBookBan", Category.MISC);
        addParameter(blockAll);
        addParameter(maxPages);
        addParameter(maxTotalChars);
    }

    public boolean shouldBlock(ItemStack stack) {
        if (!getEnabled()) return false;
        if (stack.isEmpty()) return false;
        if (!stack.is(Items.WRITTEN_BOOK) && !stack.is(Items.WRITABLE_BOOK)) return false;

        if (blockAll.getValue()) {
            warn("Blocked book opening");
            return true;
        }

        try {
            int pageCount = countPages(stack);
            if (pageCount > maxPages.getValue().intValue()) {
                warn("Book has " + pageCount + " pages (limit: " + maxPages.getValue().intValue() + ")");
                return true;
            }

            int totalChars = getTotalChars(stack);
            if (totalChars > maxTotalChars.getValue().intValue()) {
                warn("Book has " + totalChars + " chars (limit: " + maxTotalChars.getValue().intValue() + ")");
                return true;
            }
        } catch (Exception e) {
            warn("Suspicious book data detected");
            return true;
        }

        return false;
    }

    private int countPages(ItemStack stack) {
        if (stack.is(Items.WRITABLE_BOOK)) {
            var content = stack.get(net.minecraft.core.component.DataComponents.WRITABLE_BOOK_CONTENT);
            if (content != null) return content.pages().size();
        } else if (stack.is(Items.WRITTEN_BOOK)) {
            var content = stack.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
            if (content != null) {
                var pages = content.pages();
                return pages != null ? pages.size() : 0;
            }
        }
        return 0;
    }

    private int getTotalChars(ItemStack stack) {
        int total = 0;
        if (stack.is(Items.WRITABLE_BOOK)) {
            var content = stack.get(net.minecraft.core.component.DataComponents.WRITABLE_BOOK_CONTENT);
            if (content != null) {
                for (var page : content.pages()) {
                    total += page.raw().length();
                }
            }
        } else if (stack.is(Items.WRITTEN_BOOK)) {
            var content = stack.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
            if (content != null) {
                var pages = content.pages();
                if (pages != null) {
                    for (var page : pages) {
                        total += page.raw().getString().length();
                    }
                }
            }
        }
        return total;
    }

    private void warn(String reason) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.literal("§7[§cRaveX§7] §eAntiBookBan blocked book: §f" + reason),
                    false
            );
        }
    }
}
