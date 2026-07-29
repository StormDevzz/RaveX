package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.PlayerUtility;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "Speed", category = "Movement")
public class Speed {
public static boolean cancelVertical = false;
    public static float matrixTimer = 1.0f;
    @Parameter(name = "Mode", modes = {"Vanilla", "Strafe", "StrafeStrict", "NCP", "NCPStrict", "Matrix", "Grim", "GrimStrict"})
    public String mode = "Vanilla";
    @Parameter(name = "Speed", min = 0.5, max = 5.0, step = 0.1)
    public double speed = 1.5;
    @Parameter(name = "StrafeJump")
    public boolean strafeJump = true;
    @Parameter(name = "AutoJump")
    public boolean autoJump = true;
    @Parameter(name = "SpeedLimit", min = 0.1, max = 1.0, step = 0.01)
    public double speedLimit = 0.28;
    @Parameter(name = "GrimBoost", min = 0.1, max = 2.0, step = 0.1)
    public double grimBoost = 1.0;
    @Parameter(name = "InputMul", min = 1.0, max = 3.0, step = 0.1, visible = "mode=Matrix")
    public double matrixInputMul = 1.3;
    @Parameter(name = "SSCap", min = 0.1, max = 1.0, step = 0.01, visible = "mode=StrafeStrict")
    public double strafeStrictCap = 0.44;
    @Parameter(name = "SSTimer", visible = "mode=StrafeStrict")
    public boolean strafeStrictTimer = true;

    public void onTick() {
        if (!"Matrix".equals(mode)) {
            return;
        }
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;

        if (!PlayerUtility.isSprinting(mc.player) && !mc.player.isCrouching()
            && (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown())) {
            mc.player.setSprinting(true);
        }

        if (mc.player.onGround() && PlayerUtility.isSprinting(mc.player)) {
            mc.player.jumpFromGround();
        }
    }




}