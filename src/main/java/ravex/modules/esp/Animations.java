package ravex.modules.esp;

import net.minecraft.client.Minecraft;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.NativeLoader;

public class Animations extends Module {
    public static final Animations INSTANCE = new Animations();

    public final ModeParameter mode = new ModeParameter("Mode", "Slide",
        java.util.List.of("Vanilla", "Slide", "Spin", "Bounce", "None"));
    public final BooleanParameter modifySneak = new BooleanParameter("ModifySneak", true);
    public final ModeParameter sneakMode = new ModeParameter("SneakMode", "Slide",
        java.util.List.of("Vanilla", "Slide", "None"));
    public final NumberParameter speed = new NumberParameter("Speed", 1.0, 0.1, 5.0, 0.1);
    public final BooleanParameter smooth = new BooleanParameter("Smooth", true);

    private static boolean nativeAvailable = false;
    private float animationOffset;
    private float prevAnimationOffset;

    static {
        try {
            nativeAvailable = NativeLoader.loadLibrary("ravex_animations");
        } catch (Throwable t) {
            nativeAvailable = false;
        }
    }

    private Animations() {
        super("Animations", Category.RENDER);
        addParameter(mode);
        addParameter(modifySneak);
        addParameter(sneakMode);
        addParameter(speed);
        addParameter(smooth);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float spd = speed.getValue().floatValue() * 0.05f;
        float walkSpd = mc.player.walkAnimation.speed();
        if (nativeAvailable) {
            animationOffset = nativeUpdateAnimation(animationOffset, spd, walkSpd, smooth.getValue());
        } else {
            prevAnimationOffset = animationOffset;
            animationOffset += spd;
            if (animationOffset > 1.0f) animationOffset -= 1.0f;
        }
    }

    public float getAnimationOffset(float partialTick) {
        return prevAnimationOffset + (animationOffset - prevAnimationOffset) * partialTick;
    }

    public boolean shouldModifySneak() {
        return modifySneak.getValue() && !sneakMode.getValue().equals("Vanilla");
    }

    private static native float nativeUpdateAnimation(float current, float speed, float walkSpeed, boolean smooth);
}
