package ravex.modules.esp;

import net.minecraft.client.Minecraft;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

public class Blur extends Module {
    public static final Blur INSTANCE = new Blur();

    public final ModeParameter mode = new ModeParameter("Mode", "All",
        java.util.List.of("All", "Inventory", "GUI"));
    public final NumberParameter strength = new NumberParameter("Strength", 8, 2, 24, 1);
    public final BooleanParameter inventoryOnly = new BooleanParameter("InventoryOnly", false);

    private boolean blurActive;

    private Blur() {
        super("Blur", Category.RENDER);
        addParameter(mode);
        addParameter(strength);
        addParameter(inventoryOnly);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            applyBlur(mc);
        }
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        removeBlur(mc);
    }

    public void applyBlur(Minecraft mc) {
        blurActive = true;
    }

    public void removeBlur(Minecraft mc) {
        blurActive = false;
    }

    public boolean isBlurActive() {
        return blurActive;
    }

    public boolean shouldBlur() {
        if (!getEnabled()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) return false;

        String m = mode.getValue();
        if (m.equals("Inventory")) {
            return mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
        }
        if (m.equals("GUI")) {
            return mc.screen != null;
        }
        return true;
    }
}
