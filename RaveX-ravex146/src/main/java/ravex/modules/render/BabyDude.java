package ravex.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BabyDude extends Module {
    public static final BabyDude INSTANCE = new BabyDude();

    public final ModeParameter target = new ModeParameter("Target", "All", java.util.List.of("All", "Others", "Self"));
    public final NumberParameter scale = new NumberParameter("Scale", 0.5, 0.2, 1.0, 0.05);

    public final Map<Object, Float> stateScaleMap = new ConcurrentHashMap<>();

    private BabyDude() {
        super("BabyDude", Category.RENDER);
        addParameter(target);
        addParameter(scale);
    }

    public boolean shouldScale(Player player) {
        if (!getEnabled()) return false;

        Minecraft mc = Minecraft.getInstance();
        boolean isSelf = (player == mc.player);
        String t = target.getValue();

        if (t.equals("Self")) {
            return isSelf;
        } else if (t.equals("Others")) {
            return !isSelf;
        } else {
            return true;
        }
    }
}
