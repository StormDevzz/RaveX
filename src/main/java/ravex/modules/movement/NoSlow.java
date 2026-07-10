package ravex.modules.movement;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import java.util.List;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
public class NoSlow extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Grim",
            List.of("Vanilla", "NCP", "Grim", "GrimStrict", "Matrix", "GrimAlternative", "GrimV3"));
    public final ravex.parameter.BooleanParameter items = new ravex.parameter.BooleanParameter("Items", true);
    public final ravex.parameter.BooleanParameter blocks = new ravex.parameter.BooleanParameter("Blocks", true);
    public final ravex.parameter.BooleanParameter sneaking = new ravex.parameter.BooleanParameter("Sneaking", true);
    public final ravex.parameter.BooleanParameter ice = new ravex.parameter.BooleanParameter("Ice", false);
    public final NumberParameter altInterval = new NumberParameter("AltInterval", 4.0, 2.0, 20.0, 1.0);
    public final ModeParameter altAction = new ModeParameter("AltAction", "Packet",
            List.of("Packet", "Alternate"));
    public final NumberParameter v3Grace = new NumberParameter("V3Grace", 2.0, 1.0, 10.0, 1.0);
    public final NumberParameter v3Forward = new NumberParameter("V3Forward", 0.24, 0.05, 1.0, 0.05);
    public final NumberParameter v3Strafe = new NumberParameter("V3Strafe", 0.24, 0.05, 1.0, 0.05);
    public final NumberParameter v3Interval = new NumberParameter("V3Interval", 4, 1, 20, 1);

    private long matrixNextSwap = 0;
    private int altTicks = 0;
    private boolean altSlowPhase = false;
    private int v3Ticks = 0;

    private NoSlow() {
        super("NoSlow");
        altInterval.setVisible(() -> "GrimAlternative".equals(mode.getValue()));
        altAction.setVisible(() -> "GrimAlternative".equals(mode.getValue()));
        v3Grace.setVisible(() -> "GrimV3".equals(mode.getValue()));
        v3Forward.setVisible(() -> "GrimV3".equals(mode.getValue()));
        v3Strafe.setVisible(() -> "GrimV3".equals(mode.getValue()));
        v3Interval.setVisible(() -> "GrimV3".equals(mode.getValue()));
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!getEnabled()) return;
        String modeVal = mode.getValue();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        if ("Matrix".equals(modeVal)) {
            if (!mc.player.isUsingItem()) return;
            long now = System.currentTimeMillis();
            if (now < matrixNextSwap) return;
            matrixNextSwap = now + 250;
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ZERO, Direction.DOWN
            ));
            return;
        }

        if ("GrimAlternative".equals(modeVal)) {
            if (!mc.player.isUsingItem()) {
                altTicks = 0;
                return;
            }
            altTicks++;
            String action = altAction.getValue();
            int interval = altInterval.getValue().intValue();

            if ("Packet".equals(action)) {
                if (altTicks < interval) return;
                altTicks = 0;
                InteractionHand hand = mc.player.getUsedItemHand();
                mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                    BlockPos.ZERO, Direction.DOWN, 0
                ));
                mc.player.connection.send(new ServerboundUseItemPacket(
                    hand, 0, mc.player.getYRot(), mc.player.getXRot()
                ));
            } else {
                altSlowPhase = altTicks % 2 == 1;
                if (altTicks >= Math.max(2, interval * 2)) altTicks = 0;
            }
            return;
        }

        if ("GrimV3".equals(modeVal)) {
            if (!mc.player.isUsingItem()) {
                v3Ticks = 0;
                return;
            }
            v3Ticks++;

            int grace = v3Grace.getValue().intValue();
            int interval = v3Interval.getValue().intValue();

            if (v3Ticks >= grace + interval) {
                v3Ticks = grace;
            }
        }
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
    public boolean isSlowPhase() {
        return altSlowPhase;
    }

    public boolean isInGrace() {
        if (!"GrimV3".equals(mode.getValue())) return false;
        return v3Ticks <= v3Grace.getValue().intValue();
    }

    public float getV3Forward() {
        return v3Forward.getValue().floatValue();
    }

    public float getV3Strafe() {
        return v3Strafe.getValue().floatValue();
    }

    public boolean isV3Active() {
        return getEnabled() && "GrimV3".equals(mode.getValue());
    }
}
