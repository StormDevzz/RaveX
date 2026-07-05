package ravex.modules.movement;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import java.util.List;
import ravex.utility.nativelib.NativeLibrary;
public class NoFall extends Module {
    public static final NoFall INSTANCE = new NoFall();
    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla", List.of("Vanilla", "NCP", "Grim"));
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_nofall");
    static {
        NATIVE.load();
    }
    public static native boolean nativeCalculateNoFall(
        String mode,
        double fallDistance,
        double currentY,
        boolean currentOnGround,
        double[] outData
    );
    public static boolean handleNoFall(String mode, double fallDistance, double currentY, boolean currentOnGround, double[] outData) {
        if (NATIVE.isLoaded()) {
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

}
