package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;

public class NoPush extends Module {
    public static final NoPush INSTANCE = new NoPush();

    public final ModeParameter mode = new ModeParameter("Mode", "All", java.util.List.of("All", "Players", "Mobs", "Players+Mobs", "Items"));
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter mobs = new BooleanParameter("Mobs", true);
    public final BooleanParameter items = new BooleanParameter("Items", true);
    public final BooleanParameter water = new BooleanParameter("Water", false);

    private NoPush() {
        super("NoPush", Category.MOVEMENT);
        addParameter(mode);
        addParameter(players);
        addParameter(mobs);
        addParameter(items);
        addParameter(water);
        players.setVisible(() -> "All".equals(mode.getValue()) || "Players".equals(mode.getValue()) || "Players+Mobs".equals(mode.getValue()));
        mobs.setVisible(() -> "All".equals(mode.getValue()) || "Mobs".equals(mode.getValue()) || "Players+Mobs".equals(mode.getValue()));
        items.setVisible(() -> "All".equals(mode.getValue()) || "Items".equals(mode.getValue()));
        water.setVisible(() -> "All".equals(mode.getValue()));
    }

    public boolean shouldCancelPush(net.minecraft.world.entity.Entity self, net.minecraft.world.entity.Entity other) {
        if (!getEnabled()) return false;

        String m = mode.getValue();
        if ("All".equals(m)) return true;

        boolean selfPlayer = self instanceof net.minecraft.client.player.LocalPlayer;
        boolean selfMob = self instanceof net.minecraft.world.entity.monster.Monster;
        boolean otherPlayer = other instanceof net.minecraft.client.player.LocalPlayer;
        boolean otherMob = other instanceof net.minecraft.world.entity.monster.Monster;
        boolean otherItem = other instanceof net.minecraft.world.entity.item.ItemEntity;

        if (!selfPlayer && !selfMob) return false;

        switch (m) {
            case "Players":
                return otherPlayer && players.getValue();
            case "Mobs":
                return otherMob && mobs.getValue();
            case "Players+Mobs":
                return (otherPlayer && players.getValue()) || (otherMob && mobs.getValue());
            case "Items":
                return otherItem && items.getValue();
        }
        return false;
    }

    public boolean shouldCancelPush() {
        if (!getEnabled()) return false;
        String m = mode.getValue();
        if ("All".equals(m)) return true;
        return false;
    }
}
