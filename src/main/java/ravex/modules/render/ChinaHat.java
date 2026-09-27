package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import org.joml.Matrix4f;
import ravex.manager.FriendManager;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.utility.render.HatUtility;

@Module(name = "ChinaHat", category = "Render")
public class ChinaHat {
public static final ChinaHat INSTANCE = new ChinaHat();

    @Parameter(name = "Color", color = true)
    public int color = 0xFFFFFFFF;
    @Parameter(name = "Self")
    public boolean self = true;
    @Parameter(name = "Friends")
    public boolean friends = true;
    @Parameter(name = "Players")
    public boolean players = true;
    @Parameter(name = "HideFirstPerson")
    public boolean hideFirstPerson = true;

    public static void render(Matrix4f modelViewMatrix, net.minecraft.world.phys.Vec3 camPos, float partialTick) {
        ChinaHat ch = Modules.get(ChinaHat.class);
        if (ch == null || !Modules.enabled(ChinaHat.class)) {
            return;
        }

        var mc = MinecraftWrapper.getWrapper();
        var level = mc.getLevel();
        if (level == null) {
            return;
        }

        boolean firstPerson = mc.getOptions().getCameraType().isFirstPerson();
        var selfPlayer = mc.getPlayer();

        for (net.minecraft.world.entity.player.Player player : level.players()) {
            if (player.isRemoved() || !player.isAlive()) continue;

            boolean isSelf = player == selfPlayer;
            if (isSelf) {
                if (!ch.self) continue;
                if (ch.hideFirstPerson && firstPerson) continue;
            } else if (FriendManager.INSTANCE.isFriend(player.getName().getString())) {
                if (!ch.friends) continue;
            } else {
                if (!ch.players) continue;
            }

            double tx = player.xo + (player.getX() - player.xo) * partialTick;
            double ty = player.yo + (player.getY() - player.yo) * partialTick;
            double tz = player.zo + (player.getZ() - player.zo) * partialTick;
            double headY = ty + player.getBbHeight();

            HatUtility.renderHat(modelViewMatrix, camPos.x, camPos.y, camPos.z, tx, headY, tz, ch.color);
        }
    }
}
