package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import ravex.utility.player.SwingUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;

import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "NoSlow", category = "Movement")
public class NoSlow implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Vanilla", "NCP", "Grim", "GrimStrict", "Matrix", "GrimAlternative", "GrimV3"})
    public String mode = "Grim";
    @Parameter(name = "Items")
    public boolean items = true;
    @Parameter(name = "Blocks")
    public boolean blocks = true;
    @Parameter(name = "Sneaking")
    public boolean sneaking = true;
    @Parameter(name = "Ice")
    public boolean ice = false;
    @Parameter(name = "AltInterval", min = 2.0, max = 20.0, step = 1.0)
    public double altInterval = 4.0;
    @Parameter(name = "AltAction", modes = {"Packet", "Alternate"})
    public String altAction = "Packet";
    @Parameter(name = "V3Grace", min = 1.0, max = 10.0, step = 1.0)
    public double v3Grace = 2.0;
    @Parameter(name = "V3Forward", min = 0.05, max = 1.0, step = 0.05)
    public double v3Forward = 0.24;
    @Parameter(name = "V3Strafe", min = 0.05, max = 1.0, step = 0.05)
    public double v3Strafe = 0.24;
    @Parameter(name = "V3Interval", min = 1, max = 20, step = 1)
    public double v3Interval = 4;

    // Matrix mode parameters
    @Parameter(name = "SwapInterval", min = 1.0, max = 8.0, step = 1.0)
    public double matrixSwapInterval = 3.0;
    @Parameter(name = "VelocityScale", min = 0.5, max = 2.0, step = 0.01)
    public double matrixVelocityScale = 1.15;
    @Parameter(name = "InputScale", min = 0.5, max = 2.0, step = 0.05)
    public double matrixInputScale = 1.0;

    private int matrixSwapTicks = 0;
    private int altTicks = 0;
    private boolean altSlowPhase = false;
    private int v3Ticks = 0;

    private NoSlow() {
        
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("NoSlow").getEnabled()) return;
        String modeVal = mode;
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        if ("Matrix".equals(modeVal)) {
            if (!mc.player.isUsingItem()) {
                matrixSwapTicks = 0;
                return;
            }

            int interval = (int) matrixSwapInterval;
            boolean isMoving = mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.0001;

            matrixSwapTicks++;
            if (matrixSwapTicks >= interval) {
                matrixSwapTicks = 0;
                // Swap on this tick: resets server's "using item" state
                mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.DOWN
                ));
                return;
            }

            // On non-swap ticks, boost velocity to compensate for slowdown
            if (isMoving) {
                double scale = matrixVelocityScale;
                net.minecraft.world.phys.Vec3 motion = mc.player.getDeltaMovement();
                mc.player.setDeltaMovement(
                    motion.x * scale,
                    motion.y,
                    motion.z * scale
                );
            }
            return;
        }

        if ("GrimAlternative".equals(modeVal)) {
            if (!mc.player.isUsingItem()) {
                altTicks = 0;
                return;
            }
            altTicks++;
            String action = altAction;
            int interval = (int) altInterval;

            if ("Packet".equals(action)) {
                if (altTicks < interval) return;
                altTicks = 0;
                net.minecraft.world.InteractionHand hand = mc.player.getUsedItemHand();
                mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                    net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.DOWN, 0
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

            int grace = (int) v3Grace;
            int interval = (int) v3Interval;

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
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoSlow").getEnabled();
    }

    public static NoSlow itz() {
        return ravex.manager.ModuleManager.delegate(NoSlow.class);
    }

    public boolean isSlowPhase() {
        return altSlowPhase;
    }

    public boolean isInGrace() {
        if (!"GrimV3".equals(mode)) return false;
        return v3Ticks <= (int) v3Grace;
    }

    public float getV3Forward() {
        return (float) v3Forward;
    }

    public float getV3Strafe() {
        return (float) v3Strafe;
    }

    public boolean isV3Active() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoSlow").getEnabled() && "GrimV3".equals(mode);
    }

    public boolean isMatrixActive() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoSlow").getEnabled() && "Matrix".equals(mode);
    }

    public float getMatrixInputScale() {
        return (float) matrixInputScale;
    }


}