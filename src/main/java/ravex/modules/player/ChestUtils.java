package ravex.modules.player;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.player.ContainerUtility;
import ravex.mixin.player.AccessorContainerScreen;
import java.util.ArrayList;
import java.util.List;
public class ChestUtils extends Module {
    public static final ChestUtils INSTANCE = new ChestUtils();
    public final BooleanParameter steal   = new BooleanParameter("Steal", true);
    public final BooleanParameter dump    = new BooleanParameter("Dump",  true);
    public final BooleanParameter fill    = new BooleanParameter("Fill",  true);
    public final BooleanParameter dropAll = new BooleanParameter("Drop All", true);
    private static final int BTN_W = 60, BTN_H = 20, BTN_GAP = 3;

    public void onRenderButtons(AbstractContainerScreen<?> screen, GuiGraphics graphics, int mouseX, int mouseY) {
        if (!getEnabled() || !ContainerUtility.isChestLike(screen.getMenu())) return;
        var acc = (AccessorContainerScreen) screen;
        int startX = acc.getLeftPos() + acc.getImageWidth() + 5, startY = acc.getTopPos();
        List<ButtonDef> btns = getButtons();
        for (int i = 0; i < btns.size(); i++)
            drawVanillaButton(graphics, btns.get(i).label(), startX, startY + i * (BTN_H + BTN_GAP), mouseX, mouseY);
    }
    public boolean onMouseClicked(AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (!getEnabled() || !ContainerUtility.isChestLike(screen.getMenu())) return false;
        var acc = (AccessorContainerScreen) screen;
        int startX = acc.getLeftPos() + acc.getImageWidth() + 5, startY = acc.getTopPos();
        List<ButtonDef> btns = getButtons();
        for (int i = 0; i < btns.size(); i++) {
            int by = startY + i * (BTN_H + BTN_GAP);
            if (mouseX >= startX && mouseX <= startX + BTN_W && mouseY >= by && mouseY <= by + BTN_H) {
                handleAction(screen, btns.get(i).action()); return true;
            }
        }
        return false;
    }
    private void drawVanillaButton(GuiGraphics graphics, String label, int x, int y, int mouseX, int mouseY) {
        boolean h = mouseX >= x && mouseX <= x + BTN_W && mouseY >= y && mouseY <= y + BTN_H;
        int topCol = h ? 0xFFBEBEBE : 0xFFA0A0A0, botCol = h ? 0xFF6E6E6E : 0xFF505050, bgCol = h ? 0xFF8C8C8C : 0xFF6C6C6C;
        graphics.fill(x, y, x + BTN_W, y + BTN_H, bgCol);
        graphics.fill(x, y, x + BTN_W, y + 1, topCol);
        graphics.fill(x, y, x + 1, y + BTN_H, topCol);
        graphics.fill(x, y + BTN_H - 1, x + BTN_W, y + BTN_H, botCol);
        graphics.fill(x + BTN_W - 1, y, x + BTN_W, y + BTN_H, botCol);
        int tw = Minecraft.getInstance().font.width(label);
        graphics.drawString(Minecraft.getInstance().font, label, x + (BTN_W - tw) / 2, y + (BTN_H - 8) / 2, 0xFFFFFFFF, true);
    }
    private void handleAction(AbstractContainerScreen<?> screen, String action) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        var menu = screen.getMenu();
        switch (action) {
            case "STEAL" -> ContainerUtility.quickMoveAll(mc, player, ContainerUtility.getContainerSlots(menu));
            case "DUMP"  -> ContainerUtility.quickMoveAll(mc, player, ContainerUtility.getPlayerSlots(menu));
            case "FILL"  -> ContainerUtility.fillFromContainer(mc, player, menu);
            case "DROP"  -> ContainerUtility.throwAll(mc, player, ContainerUtility.getContainerSlots(menu));
        }
    }
    private List<ButtonDef> getButtons() {
        List<ButtonDef> list = new ArrayList<>();
        if (steal.getValue())   list.add(new ButtonDef("Steal", "STEAL"));
        if (dump.getValue())    list.add(new ButtonDef("Dump", "DUMP"));
        if (fill.getValue())    list.add(new ButtonDef("Fill", "FILL"));
        if (dropAll.getValue()) list.add(new ButtonDef("Drop", "DROP"));
        return list;
    }
    record ButtonDef(String label, String action) {}
}
