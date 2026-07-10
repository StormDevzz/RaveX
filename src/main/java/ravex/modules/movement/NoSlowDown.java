package ravex.modules.movement;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import java.util.List;
import ravex.utility.nativelib.NativeLibrary;
public class NoSlowDown extends Module {
    public static final NoSlowDown INSTANCE = new NoSlowDown();
    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla",
            List.of("Vanilla", "Grim", "NCP"));
    public final ravex.parameter.BooleanParameter items = new ravex.parameter.BooleanParameter("Items", true);
    public final ravex.parameter.BooleanParameter blocks = new ravex.parameter.BooleanParameter("Blocks", true);
    public final ravex.parameter.BooleanParameter sneaking = new ravex.parameter.BooleanParameter("Sneaking", true);
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_noslow");
    static {
        NATIVE.load();
    }
    public static native float nativeGetBlockFriction(String blockId, float defaultFriction);
    public static float getBlockFriction(String blockId, float defaultFriction) {
        if (NATIVE.isLoaded()) {
            try {
                return nativeGetBlockFriction(blockId, defaultFriction);
            } catch (UnsatisfiedLinkError | Exception e) {
            }
        }
        if ("minecraft:slime_block".equals(blockId) ||
            "minecraft:honey_block".equals(blockId) ||
            "minecraft:soul_sand".equals(blockId)) {
            return 0.6f;
        }
        return defaultFriction;
    }

}
