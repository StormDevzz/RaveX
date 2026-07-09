package ravex.modules.movement;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import java.util.List;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
public class NoSlow extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Grim",
            List.of("Vanilla", "NCP", "Grim", "GrimStrict", "Matrix"));
    public final ravex.parameter.BooleanParameter items = new ravex.parameter.BooleanParameter("Items", true);
    public final ravex.parameter.BooleanParameter blocks = new ravex.parameter.BooleanParameter("Blocks", true);
    public final ravex.parameter.BooleanParameter sneaking = new ravex.parameter.BooleanParameter("Sneaking", true);
    public final ravex.parameter.BooleanParameter ice = new ravex.parameter.BooleanParameter("Ice", false);

    private long matrixNextSwap = 0;

    private NoSlow() {
        super("NoSlow");
    }

    @Override
    public void onTick() {
        String modeVal = mode.getValue();
        if (!"Matrix".equals(modeVal)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        if (!mc.player.isUsingItem()) return;
        long now = System.currentTimeMillis();
        if (now < matrixNextSwap) return;
        matrixNextSwap = now + 250;
        mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
            net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
            net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.DOWN
        ));
    }

    public static float getBlockFriction(String blockId, float defaultFriction) {
        if ("minecraft:slime_block".equals(blockId) || 
            "minecraft:honey_block".equals(blockId) || 
            "minecraft:soul_sand".equals(blockId)) {
            return 0.6f;
        }
        return defaultFriction;
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(NoSlow.class);
    }
    public static NoSlow itz() {
        return ModuleManager.get(NoSlow.class);
    }
}
