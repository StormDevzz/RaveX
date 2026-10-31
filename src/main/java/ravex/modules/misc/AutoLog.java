package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.combat.TotemPopEvent;
import ravex.utility.misc.EntityUtility;
import ravex.utility.player.PlayerUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "AutoLog", category = "Misc")
public class AutoLog {
    @Parameter(name = "LowHealth")
    public boolean onLowHealth = true;
    @Parameter(name = "MinHP", min = 1.0, max = 20.0, step = 0.5, visible = "onLowHealth")
    public double healthLimit = 6.0;
    @Parameter(name = "PlayerNearby")
    public boolean onPlayerNearby = false;
    @Parameter(name = "Range", min = 4.0, max = 64.0, step = 1.0, visible = "onPlayerNearby")
    public double playerRange = 16.0;
    @Parameter(name = "OnTotemPop")
    public boolean onTotemPop = true;
    @Parameter(name = "MaxTotemPops", min = 1.0, max = 10.0, step = 1.0, visible = "onTotemPop")
    public double maxTotemPops = 1.0;
    @Parameter(name = "EntityNearby")
    public boolean onEntityNearby = false;
    @Parameter(name = "Entity", visible = "onEntityNearby")
    public String entityType = "";
    @Parameter(name = "EntityRange", min = 4.0, max = 64.0, step = 1.0, visible = "onEntityNearby")
    public double entityRange = 16.0;
    private int totemPops = 0;
    public void onEnable() {
        totemPops = 0;
    }
    @Subscribe
    public void onTotemPop(TotemPopEvent event) {
        if (!Modules.enabled(AutoLog.class) || !onTotemPop) return;
        var mc = MinecraftWrapper.getWrapper();
        if (event.getPlayer() != mc.getPlayer()) return;
        totemPops++;
        if (totemPops >= (int) maxTotemPops)
            disconnect("TotemPopped(" + totemPops + ")");
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        if (onLowHealth && PlayerUtility.getHealth(player) <= healthLimit) {
            disconnect("LowHealthTriggered(" + PlayerUtility.getHealth(player) + " HP)");
            return;
        }
        if (onEntityNearby && !entityType.isEmpty()) {
            for (var e : mc.getLevel().entitiesForRendering()) {
                if (e == player || !e.isAlive()) continue;
                String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
                if (!id.equals(entityType)) continue;
                double dist = e.distanceTo(player);
                if (dist <= entityRange) {
                    disconnect("Entity(" + id + ") nearby (" + String.format("%.1f", dist) + "m)");
                    return;
                }
            }
        }
        for (var other : mc.getLevel().players()) {
            if (other == player) continue;
            double dist = EntityUtility.distanceToPlayer(other);
            if (onPlayerNearby && dist <= playerRange) {
                disconnect("net.minecraft.world.entity.player.Player " + other.getGameProfile().name() + " is too close (" + String.format("%.1f", dist) + "m)");
                return;
            }
        }
    }
    private void disconnect(String reason) {
        var mc = MinecraftWrapper.getWrapper();
        var connection = mc.getConnection();
        if (connection != null) {
            connection.getConnection().disconnect(net.minecraft.network.chat.Component.literal("§c[RaveX AutoLog] §f" + reason));
        }
        Modules.setEnabled(AutoLog.class, false);
    }

    // opens the entity picker, wired to the SelectEntity toggle in the gui
    public static void openBrowser() {
        var mc = MinecraftWrapper.getWrapper();
        AutoLog inst = Modules.get(AutoLog.class);
        var screen = mc.getCurrentScreen();
        if (inst == null || screen instanceof ravex.gui.browser.AutoLogBrowserScreen) return;
        mc.setScreen(new ravex.gui.browser.AutoLogBrowserScreen(
                screen,
                id -> false,
                (id, on) -> {},
                id -> inst.entityType.equals(id.toString()),
                (id, on) -> {
                    if (on) inst.entityType = id.toString();
                    else if (inst.entityType.equals(id.toString())) inst.entityType = "";
                },
                () -> inst.entityType = ""));
    }
}
