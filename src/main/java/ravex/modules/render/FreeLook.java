package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.CameraType;
@Module(name = "FreeLook", category = "Render")
public class FreeLook {
    @Parameter(name = "Mode", modes = {"Player", "Camera"})
    public String mode = "Player";
    @Parameter(name = "Zoom")
    public boolean zoom = false;
    @Parameter(name = "ZoomDistance", min = 1.0, max = 12.0, step = 0.5, visible = "zoom")
    public double zoomDistance = 4.0;
    private float lookYaw = 0.0f;
    private float lookPitch = 0.0f;
    private int originalPerspective = 0;
    private float smoothZoom = 4.0f;
    private long lastZoomMs = 0;
    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null) {
            lookYaw = mc.getPlayer().getYRot();
            lookPitch = mc.getPlayer().getXRot();
            originalPerspective = mc.getOptions().getCameraType().ordinal();
            mc.getOptions().setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        smoothZoom = (float) zoomDistance;
        lastZoomMs = 0;
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getOptions() != null) {
            var types = CameraType.values();
            if (originalPerspective >= 0 && originalPerspective < types.length) {
                mc.getOptions().setCameraType(types[originalPerspective]);
            }
        }
        zoomDistance = 4.0;
        smoothZoom = 4.0f;
        lastZoomMs = 0;
    }
    public boolean isCameraMode() {
        return "Camera".equals(mode);
    }
    public boolean isPlayerMode() {
        return "Player".equals(mode);
    }
    public void adjustZoom(double yOffset) {
        if (!zoom) return;
        if (yOffset > 0) zoomDistance = Math.min(12.0, zoomDistance - 0.5);
        else if (yOffset < 0) zoomDistance = Math.max(1.0, zoomDistance + 0.5);
    }
    public float getCameraDistance() {
        long now = System.currentTimeMillis();
        if (lastZoomMs == 0) lastZoomMs = now;
        float deltaMs = now - lastZoomMs;
        lastZoomMs = now;
        if (deltaMs > 100) deltaMs = 16;
        float target = (float) zoomDistance;
        float t = (float) (1.0 - Math.exp(-deltaMs * 0.018));
        if (t > 1f) t = 1f;
        smoothZoom += (target - smoothZoom) * t;
        if (Math.abs(target - smoothZoom) < 0.002f) smoothZoom = target;
        return smoothZoom;
    }
    public void turn(double yRot, double xRot) {
        lookYaw += (float) yRot;
        lookPitch += (float) xRot;
        lookPitch = Math.max(-90.0f, Math.min(90.0f, lookPitch));
    }
    public float getLookYaw() {
        return lookYaw;
    }
    public float getLookPitch() {
        return lookPitch;
    }
}
