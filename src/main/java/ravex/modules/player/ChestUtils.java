package ravex.modules.player;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.player.ContainerUtility;
import java.util.ArrayList;
import java.util.List;
public class ChestUtils extends Module {
    public static final ChestUtils INSTANCE = new ChestUtils();
    public final BooleanParameter steal   = new BooleanParameter("Steal", true);
    public final BooleanParameter dump    = new BooleanParameter("Dump",  true);
    public final BooleanParameter fill    = new BooleanParameter("Fill",  true);
    public final BooleanParameter dropAll = new BooleanParameter("DropAll", true);

    public void onRenderButtons(AbstractContainerScreen<?> screen, net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        if (!getEnabled() || !ContainerUtility.isChestLike(screen.getMenu())) return;
        int startX = ContainerUtility.getButtonStartX(screen), startY = ContainerUtility.getButtonStartY(screen);
        List<ButtonDef> btns = getButtons();
        for (int i = 0; i < btns.size(); i++) {
            int bx = startX, by = startY + i * (ContainerUtility.CHEST_BTN_H + ContainerUtility.CHEST_BTN_GAP);
            ContainerUtility.drawChestButton(graphics, btns.get(i).label(), bx, by, ContainerUtility.isMouseOverButton(mouseX, mouseY, bx, by));
        }
    }
    public boolean onMouseClicked(AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (!getEnabled() || !ContainerUtility.isChestLike(screen.getMenu())) return false;
        int startX = ContainerUtility.getButtonStartX(screen), startY = ContainerUtility.getButtonStartY(screen);
        List<ButtonDef> btns = getButtons();
        for (int i = 0; i < btns.size(); i++) {
            int by = startY + i * (ContainerUtility.CHEST_BTN_H + ContainerUtility.CHEST_BTN_GAP);
            if (ContainerUtility.isMouseOverButton(mouseX, mouseY, startX, by)) {
                handleAction(screen, btns.get(i).action()); return true;
            }
        }
        return false;
    }
    private void handleAction(AbstractContainerScreen<?> screen, String action) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
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
