package ravex.event.render;

import ravex.event.CancellableEvent;

public class FogEvent extends CancellableEvent {
    private float fogStart;
    private float fogEnd;
    private FogType type;

    public enum FogType { DEFAULT, LAVA, POWDER_SNOW, NO_FOG, CUSTOM }

    public FogEvent(FogType type, float fogStart, float fogEnd) {
        this.type = type;
        this.fogStart = fogStart;
        this.fogEnd = fogEnd;
    }

    public FogType getFogType() { return type; }
    public void setFogType(FogType type) { this.type = type; }
    public float getFogStart() { return fogStart; }
    public float getFogEnd() { return fogEnd; }
    public void setFogRange(float start, float end) { this.fogStart = start; this.fogEnd = end; }
}
