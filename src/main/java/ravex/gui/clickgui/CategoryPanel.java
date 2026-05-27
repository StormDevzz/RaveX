package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.modules.ModuleManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CategoryPanel {
    private final Category category;
    private final int x;
    private final int y;
    private final int width = 100;
    private final List<ModuleButton> allButtons = new ArrayList<>();

    public CategoryPanel(Category category, int x, int y) {
        this.category = category;
        this.x = x;
        this.y = y;
        for (Module m : ModuleManager.INSTANCE.getByCategory(category)) {
            allButtons.add(new ModuleButton(m));
        }
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, String searchQuery) {
        List<ModuleButton> visible = filterButtons(searchQuery);
        if (visible.isEmpty() && !searchQuery.isEmpty()) return;
        if (visible.isEmpty() && allButtons.isEmpty()) return;

        graphics.fill(x, y, x + width, y + 16, ColorUtility.HEADER_COLOR);

        int borderGlow = ColorUtility.getRainbowColor(category.ordinal(), 5000);
        graphics.fill(x, y - 2, x + width, y, borderGlow);

        String header = category.getDisplayName();
        graphics.drawString(font, header, x + 6, y + 4, 0xFFFFFFFF, true);

        int[] currentYOut = { y + 16 };
        for (ModuleButton btn : visible) {
            btn.render(graphics, font, x, y, width, mouseX, mouseY, currentYOut, searchQuery);
        }

        int currentY = currentYOut[0];
        graphics.fill(x - 1, y, x, currentY, ColorUtility.PANEL_BORDER_COLOR);
        graphics.fill(x + width, y, x + width + 1, currentY, ColorUtility.PANEL_BORDER_COLOR);
        graphics.fill(x, currentY, x + width, currentY + 1, ColorUtility.PANEL_BORDER_COLOR);
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        render(graphics, font, mouseX, mouseY, "");
    }

    public int getMatchCount(String query) {
        return (int) allButtons.stream()
            .filter(b -> b.getModule().getName().toLowerCase().contains(query.toLowerCase()))
            .count();
    }

    private List<ModuleButton> filterButtons(String query) {
        if (query == null || query.isEmpty()) return allButtons;
        String lower = query.toLowerCase();
        return allButtons.stream()
            .filter(b -> b.getModule().getName().toLowerCase().contains(lower))
            .collect(Collectors.toList());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, net.minecraft.client.Minecraft mc) {
        int[] currentYOut = { y + 16 };
        for (ModuleButton btn : allButtons) {
            if (btn.mouseClicked(mouseX, mouseY, button, x, currentYOut, mc)) {
                return true;
            }
        }
        return false;
    }
}
