package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import java.util.List;

public class NoSlowDown extends Module {
    public static final NoSlowDown INSTANCE = new NoSlowDown();

    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla",
            List.of("Vanilla", "Grim", "NCP"));
    public final ravex.parameter.BooleanParameter items = new ravex.parameter.BooleanParameter("Items", true);
    public final ravex.parameter.BooleanParameter blocks = new ravex.parameter.BooleanParameter("Blocks", true);
    public final ravex.parameter.BooleanParameter cobwebs = new ravex.parameter.BooleanParameter("Cobwebs", true);

    private static boolean nativeLibLoaded = false;
    static {
        try {
            nativeLibLoaded = ravex.utility.misc.NativeLoader.loadLibrary("ravex_noslow");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[NoSlow JNI] Failed to load native library: " + e.getMessage());
        }
    }

    public static native float nativeGetBlockFriction(String blockId, float defaultFriction);

    public static float getBlockFriction(String blockId, float defaultFriction) {
        if (nativeLibLoaded) {
            try {
                return nativeGetBlockFriction(blockId, defaultFriction);
            } catch (UnsatisfiedLinkError | Exception e) {
                // Fallback
            }
        }
        if ("minecraft:slime_block".equals(blockId) || 
            "minecraft:honey_block".equals(blockId) || 
            "minecraft:soul_sand".equals(blockId)) {
            return 0.6f;
        }
        return defaultFriction;
    }

    private NoSlowDown() {
        super("NoSlowDown", Category.MOVEMENT);
        addParameter(mode);
        addParameter(items);
        addParameter(blocks);
        addParameter(cobwebs);
    }
}

