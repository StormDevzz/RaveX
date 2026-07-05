package ravex.modules.misc;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.food.FoodEater;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.List;
public class AutoEat extends Module {
    public static final AutoEat INSTANCE = new AutoEat();
    public final NumberParameter threshold = new NumberParameter("Hunger", 15.0, 1.0, 20.0, 1.0);
    public final BooleanParameter priority = new BooleanParameter("BestFood", true);
    public final BooleanParameter notify = new BooleanParameter("Notify", false);
    public final ModeParameter mode = new ModeParameter("Mode", "Normal",
            List.of("Normal", "Silent", "Vanilla"));

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        float hunger = mc.player.getFoodData().getFoodLevel();
        if ("Vanilla".equals(mode.getValue())) {
            mc.options.keyUse.setDown(hunger < threshold.getValue());
            return;
        }
        if (FoodEater.INSTANCE.isEating()) {
            FoodEater.EatResult result = FoodEater.INSTANCE.tryEat();
            if (result == FoodEater.EatResult.FINISHED && notify.getValue()) {
                mc.player.displayClientMessage(
                    Component.literal("§7[§cAutoEat§7] §aDone eating"), false);
            }
            return;
        }
        if (hunger >= threshold.getValue()) return;
        FoodEater.EatResult result = FoodEater.INSTANCE.tryEat();
        if (result == FoodEater.EatResult.STARTED && notify.getValue()) {
            mc.player.displayClientMessage(
                Component.literal("§7[§cAutoEat§7] §aEating (" + (int)hunger + " hunger)"),
                false);
        }
    }
    @Override
    protected void onDisable() {
        if ("Vanilla".equals(mode.getValue())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options != null) mc.options.keyUse.setDown(false);
        }
        FoodEater.INSTANCE.reset();
    }
}
