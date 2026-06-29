package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;

import java.util.List;

public class NoFall extends Module {
    public static final NoFall INSTANCE = new NoFall();

    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla", List.of("Vanilla", "NCP", "Grim"));

    private static boolean nativeLibLoaded = false;
    static {
        try {
            nativeLibLoaded = ravex.utility.misc.NativeLoader.loadLibrary("ravex_nofall");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[NoFall JNI] Failed to load native library: " + e.getMessage());
        }
    }

    public static native boolean nativeCalculateNoFall(
        String mode,
        double fallDistance,
        double currentY,
        boolean currentOnGround,
        double[] outData
    );

    public static boolean handleNoFall(String mode, double fallDistance, double currentY, boolean currentOnGround, double[] outData) {
        if (nativeLibLoaded) {
            try {
                return nativeCalculateNoFall(mode, fallDistance, currentY, currentOnGround, outData);
            } catch (UnsatisfiedLinkError | Exception e) {
                
            }
        }
        
        if (fallDistance > 2.0) {
            outData[0] = 1.0; 
            if ("Grim".equals(mode)) {
                outData[1] = currentY + 0.0001;
            } else {
                outData[1] = currentY;
            }
            return true;
        }
        return false;
    }

    private NoFall() {
        super("NoFall", Category.MOVEMENT);
        addParameter(mode);
    }
}
