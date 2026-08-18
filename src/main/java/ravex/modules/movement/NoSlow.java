package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.network.NetworkUtility;
import ravex.utility.player.PlayerUtility;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "NoSlow", category = "Movement")
public class NoSlow {
    @Parameter(name = "Mode", modes = {"Vanilla", "NCP", "Grim", "GrimStrict", "Matrix", "GrimAlternative", "GrimV3", "FunSky"})
    public String mode = "Grim";
    @Parameter(name = "Items")
    public boolean items = true;
    @Parameter(name = "Blocks")
    public boolean blocks = true;
    @Parameter(name = "Sneaking")
    public boolean sneaking = true;
    @Parameter(name = "Ice")
    public boolean ice = false;
    @Parameter(name = "FunSkyInterval", min = 1.0, max = 20.0, step = 1.0, visible = "mode=FunSky")
    public double funSkyInterval = 4.0;
    @Parameter(name = "AltInterval", min = 2.0, max = 20.0, step = 1.0, visible = "mode=GrimAlternative")
    public double altInterval = 4.0;
    @Parameter(name = "AltAction", modes = {"Packet", "Alternate"}, visible = "mode=GrimAlternative")
    public String altAction = "Packet";
    @Parameter(name = "V3Grace", min = 1.0, max = 10.0, step = 1.0, visible = "mode=GrimV3")
    public double v3Grace = 2.0;
    @Parameter(name = "V3Forward", min = 0.05, max = 1.0, step = 0.05, visible = "mode=GrimV3")
    public double v3Forward = 0.24;
    @Parameter(name = "V3Strafe", min = 0.05, max = 1.0, step = 0.05, visible = "mode=GrimV3")
    public double v3Strafe = 0.24;
    @Parameter(name = "V3Interval", min = 1, max = 20, step = 1, visible = "mode=GrimV3")
    public double v3Interval = 4;

    @Parameter(name = "SwapInterval", min = 1.0, max = 8.0, step = 1.0, visible = "mode=Matrix")
    public double matrixSwapInterval = 3.0;
    @Parameter(name = "VelocityScale", min = 0.5, max = 2.0, step = 0.01, visible = "mode=Matrix")
    public double matrixVelocityScale = 1.15;
    @Parameter(name = "InputScale", min = 0.5, max = 2.0, step = 0.05, visible = "mode=Matrix")
    public double matrixInputScale = 1.0;

    private int matrixSwapTicks = 0;
    private int altTicks = 0;
    private boolean altSlowPhase = false;
    private int v3Ticks = 0;
    private int funSkyTicks = 0;

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!Modules.enabled(NoSlow.class)) return;
        String modeVal = mode;
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;

        if ("Matrix".equals(modeVal)) {
            if (!PlayerUtility.isUsingItem(player)) {
                matrixSwapTicks = 0;
                return;
            }

            int interval = (int) matrixSwapInterval;
            boolean isMoving = mc.getPlayerDeltaMovement().horizontalDistanceSqr() > 0.0001;

            matrixSwapTicks++;
            if (matrixSwapTicks >= interval) {
                matrixSwapTicks = 0;
                NetworkUtility.sendSwapWithOffhand();
                return;
            }

            if (isMoving) {
                double scale = matrixVelocityScale;
                var motion = mc.getPlayerDeltaMovement();
                mc.setPlayerDeltaMovement(motion.x * scale, motion.y, motion.z * scale);
            }
            return;
        }

        if ("FunSky".equals(modeVal)) {
            if (!PlayerUtility.isUsingItem(player)) {
                funSkyTicks = 0;
                return;
            }
            funSkyTicks++;
            if (funSkyTicks % (int) funSkyInterval != 0) return;
            int slot = ravex.utility.player.InventoryUtility.getSelectedSlot(player);
            NetworkUtility.sendSetCarriedItem((slot + 1) % 9);
            NetworkUtility.sendSetCarriedItem(slot);
            return;
        }

        if ("GrimAlternative".equals(modeVal)) {
            if (!PlayerUtility.isUsingItem(player)) {
                altTicks = 0;
                return;
            }
            altTicks++;
            String action = altAction;
            int interval = (int) altInterval;

            if ("Packet".equals(action)) {
                if (altTicks < interval) return;
                altTicks = 0;
                var hand = player.getUsedItemHand();
                NetworkUtility.sendReleaseUseItem();
                NetworkUtility.sendUseItem(hand, player.getYRot(), player.getXRot());
            } else {
                altSlowPhase = altTicks % 2 == 1;
                if (altTicks >= Math.max(2, interval * 2)) altTicks = 0;
            }
            return;
        }

        if ("GrimV3".equals(modeVal)) {
            if (!PlayerUtility.isUsingItem(player)) {
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
        return Modules.enabled(NoSlow.class) && "GrimV3".equals(mode);
    }

    public boolean isMatrixActive() {
        return Modules.enabled(NoSlow.class) && "Matrix".equals(mode);
    }

    public float getMatrixInputScale() {
        return (float) matrixInputScale;
    }
}
