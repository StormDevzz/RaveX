package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.PlayerUtility;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "Speed", category = "Movement")
public class Speed {
    public static float matrixTimer = 1.0f;
    @Parameter(name = "Mode", modes = {"Vanilla", "Strafe", "StrafeStrict", "NCP", "NCPStrict", "Matrix", "Grim", "GrimStrict", "Verus"})
    public String mode = "Vanilla";
    @Parameter(name = "Speed", min = 0.5, max = 5.0, step = 0.1, visible = "mode=Vanilla||mode=Strafe||mode=NCP||mode=NCPStrict||mode=Grim")
    public double speed = 1.5;
    @Parameter(name = "StrafeJump", visible = "mode=Strafe||mode=StrafeStrict")
    public boolean strafeJump = true;
    @Parameter(name = "SpeedLimit", min = 0.1, max = 1.0, step = 0.01, visible = "mode=Vanilla||mode=Strafe||mode=NCP||mode=NCPStrict||mode=Grim||mode=Verus")
    public double speedLimit = 0.28;
    @Parameter(name = "GrimBoost", min = 0.1, max = 2.0, step = 0.1, visible = "mode=Grim||mode=GrimStrict")
    public double grimBoost = 1.0;
    @Parameter(name = "NCPJump", min = 0.3, max = 0.5, step = 0.01, visible = "mode=NCP")
    public double ncpJump = 0.42;
    @Parameter(name = "StrictJump", min = 0.3, max = 0.5, step = 0.01, visible = "mode=NCPStrict")
    public double strictJump = 0.40;
    @Parameter(name = "InputMul", min = 1.0, max = 3.0, step = 0.1, visible = "mode=Matrix")
    public double matrixInputMul = 1.3;
    @Parameter(name = "SSCap", min = 0.1, max = 1.0, step = 0.01, visible = "mode=StrafeStrict")
    public double strafeStrictCap = 0.44;
    @Parameter(name = "SSTimer", visible = "mode=StrafeStrict")
    public boolean strafeStrictTimer = true;
    @Parameter(name = "VerusSpeed", min = 0.1, max = 0.5, step = 0.01, visible = "mode=Verus")
    public double verusSpeed = 0.28;

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        boolean moving = mc.isForwardKeyDown() || mc.isBackKeyDown() || mc.isLeftKeyDown() || mc.isRightKeyDown();
        if ("Verus".equals(mode)) {
            if (!player.onGround()) return;
            if (!PlayerUtility.isSprinting(player) && !player.isCrouching() && moving) {
                player.setSprinting(true);
            }
            if (PlayerUtility.isSprinting(player)) {
                player.jumpFromGround();
            }
            return;
        }
        if ("Matrix".equals(mode)) {
            if (!PlayerUtility.isSprinting(player) && !player.isCrouching() && moving) {
                player.setSprinting(true);
            }
            if (PlayerUtility.isOnGround(player) && PlayerUtility.isSprinting(player)) {
                player.jumpFromGround();
            }
            return;
        }
        if (("Strafe".equals(mode) || "StrafeStrict".equals(mode)) && strafeJump && moving && player.onGround()) {
            player.jumpFromGround();
        }
    }
}
