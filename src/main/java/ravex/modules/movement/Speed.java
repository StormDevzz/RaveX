package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.BooleanParameter;
import net.minecraft.client.Minecraft;

@ModuleInfo(name = "Speed", category = "Movement")
public class Speed extends ravex.modules.Module {
public static boolean cancelVertical = false;
    public static float matrixTimer = 1.0f;
    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla",
        java.util.List.of("Vanilla", "Strafe", "StrafeStrict", "NCP", "NCPStrict", "Matrix", "Grim", "GrimStrict"));
    public final NumberParameter speed = new NumberParameter("Speed", 1.5, 0.5, 5.0, 0.1);
    public final BooleanParameter strafeJump = new BooleanParameter("StrafeJump", true);
    public final BooleanParameter autoJump = new BooleanParameter("AutoJump", true);
    public final NumberParameter speedLimit = new NumberParameter("SpeedLimit", 0.28, 0.1, 1.0, 0.01);
    public final NumberParameter grimBoost = new NumberParameter("GrimBoost", 1.0, 0.1, 2.0, 0.1);
    public final NumberParameter matrixInputMul = new NumberParameter("InputMul", 1.3, 1.0, 3.0, 0.1);
    public final NumberParameter strafeStrictCap = new NumberParameter("SSCap", 0.44, 0.1, 1.0, 0.01);
    public final BooleanParameter strafeStrictTimer = new BooleanParameter("SSTimer", true);

    private Speed() {
        
        strafeJump.setVisible(() -> "Strafe".equals(mode.getValue()));
        grimBoost.setVisible(() -> "Grim".equals(mode.getValue()) || "GrimStrict".equals(mode.getValue()));
        autoJump.setVisible(() -> !"GrimStrict".equals(mode.getValue()) && !"Grim".equals(mode.getValue()) && !"StrafeStrict".equals(mode.getValue()));
        speedLimit.setVisible(() -> !"GrimStrict".equals(mode.getValue()) && !"StrafeStrict".equals(mode.getValue()));
        speed.setVisible(() -> !"GrimStrict".equals(mode.getValue()) && !"StrafeStrict".equals(mode.getValue()));
        matrixInputMul.setVisible(() -> "Matrix".equals(mode.getValue()));
        strafeStrictCap.setVisible(() -> "StrafeStrict".equals(mode.getValue()));
        strafeStrictTimer.setVisible(() -> "StrafeStrict".equals(mode.getValue()));
    }
    public void onTick() {
        if (!"Matrix".equals(mode.getValue())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!mc.player.isSprinting() && !mc.player.isCrouching()
            && (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown())) {
            mc.player.setSprinting(true);
        }

        if (mc.player.onGround() && mc.player.isSprinting()) {
            mc.player.jumpFromGround();
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Speed").getEnabled();
    }
    public static Speed itz() {
        return ravex.manager.ModuleManager.delegate(Speed.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}