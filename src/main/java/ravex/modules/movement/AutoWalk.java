package ravex.modules.movement;

import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.annotations.Module;

@Module(name = "AutoWalk", category = "Movement")
public class AutoWalk {
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        mc.getOptions().keyUp.setDown(true);
    }

    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getOptions() != null) {
            mc.getOptions().keyUp.setDown(false);
        }
    }
}
