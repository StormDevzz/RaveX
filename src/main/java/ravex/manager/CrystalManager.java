package ravex.manager;

import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import ravex.mcwrapper.MinecraftWrapper;
import java.util.ArrayList;
import java.util.List;

public class CrystalManager {
    public static final CrystalManager INSTANCE = new CrystalManager();

    private CrystalManager() {}

    public List<EndCrystal> getLoadedCrystals() {
        List<EndCrystal> crystals = new ArrayList<>();
        var level = MinecraftWrapper.getWrapper().getLevel();
        if (level == null) return crystals;
        for (var entity : level.entitiesForRendering()) {
            if (entity instanceof EndCrystal) {
                crystals.add((EndCrystal) entity);
            }
        }
        return crystals;
    }
}
