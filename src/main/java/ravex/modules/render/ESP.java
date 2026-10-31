package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "ESP", category = "Render")
public class ESP {
    @Parameter(name = "Mode", modes = {"Outline", "Box2D"})
    public String mode = "Outline";
    @Parameter(name = "Players")
    public boolean players = true;
    @Parameter(name = "Monsters")
    public boolean monsters = true;
    @Parameter(name = "Animals")
    public boolean animals = false;
    @Parameter(name = "Items")
    public boolean items = false;
    @Parameter(name = "Frames")
    public boolean frames = false;
    @Parameter(name = "Distance", min = 10.0, max = 300.0, step = 10.0)
    public double maxDistance = 100.0;
    @Parameter(name = "PlayerColor", color = true, visible = "players")
    public int playerColor = 0xFFFF3333;
    @Parameter(name = "MobColor", color = true, visible = "monsters")
    public int mobColor = 0xFF33FF33;
    @Parameter(name = "AnimalColor", color = true, visible = "animals")
    public int animalColor = 0xFF33FF55;
    @Parameter(name = "ItemColor", color = true, visible = "items")
    public int itemColor = 0xFFFFFF33;
    @Parameter(name = "FrameColor", color = true, visible = "frames")
    public int frameColor = 0xFFFF9933;
    @Parameter(name = "BoxWidth", min = 1.0, max = 5.0, step = 1.0, visible = "mode=Box2D")
    public double boxWidth = 3.0;
    @Parameter(name = "DistScale", visible = "mode=Box2D")
    public boolean distScale = true;

    public static boolean shouldGlow(net.minecraft.world.entity.Entity entity) {
        ESP $ = Modules.get(ESP.class);
        if ($ == null || !Modules.enabled(ESP.class) || !$.mode.equals("Outline")) return false;
        var mc = MinecraftWrapper.getWrapper();
        if (entity == mc.getPlayer()) return false;
        if (mc.getPlayer() != null && mc.getPlayer().distanceTo(entity) > $.maxDistance) return false;
        if (entity instanceof net.minecraft.world.entity.player.Player && $.players) return true;
        if (entity instanceof net.minecraft.world.entity.monster.Monster && $.monsters) return true;
        if ((entity instanceof net.minecraft.world.entity.animal.Animal || entity instanceof net.minecraft.world.entity.ambient.AmbientCreature) && $.animals) return true;
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity && $.items) return true;
        if (entity instanceof net.minecraft.world.entity.decoration.ItemFrame && $.frames) return true;
        return false;
    }
}
