package ravex.modules;

import ravex.parameter.Parameter;
import java.util.ArrayList;
import java.util.List;

public abstract class HudModule {
    private final String name;
    private boolean enabled;
    private int targetX;
    private int targetY;
    private int width;
    private int height;
    private final List<Parameter<?>> parameters = new ArrayList<>();

    
    private float displayX;
    private float displayY;
    private boolean animInitialized = false;

    public HudModule(String name, int defaultX, int defaultY, int width, int height) {
        this.name = name;
        this.targetX = defaultX;
        this.targetY = defaultY;
        this.width = width;
        this.height = height;
        this.enabled = false;
    }

    public void updateAnimation() {
        if (!animInitialized) {
            displayX = targetX;
            displayY = targetY;
            animInitialized = true;
        }
        float speed = 0.25f;
        displayX += (targetX - displayX) * speed;
        displayY += (targetY - displayY) * speed;
        if (Math.abs(targetX - displayX) < 0.3f) displayX = targetX;
        if (Math.abs(targetY - displayY) < 0.3f) displayY = targetY;
    }

    
    public float getDisplayX() { return displayX; }
    public void setDisplayX(float displayX) { this.displayX = displayX; }
    public float getDisplayY() { return displayY; }
    public void setDisplayY(float displayY) { this.displayY = displayY; }
    public boolean isAnimInitialized() { return animInitialized; }
    public void setAnimInitialized(boolean animInitialized) { this.animInitialized = animInitialized; }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return ravex.descriptions.HudDescriptions.getDescription(name);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public int getX() {
        return Math.round(displayX);
    }

    public int getY() {
        return Math.round(displayY);
    }

    public int getTargetX() {
        return targetX;
    }

    public int getTargetY() {
        return targetY;
    }

    public void setX(int x) {
        this.targetX = x;
        if (!animInitialized) {
            displayX = x;
            animInitialized = true;
        }
    }

    public void setY(int y) {
        this.targetY = y;
        if (!animInitialized) {
            displayY = y;
            animInitialized = true;
        }
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public List<Parameter<?>> getParameters() {
        return parameters;
    }

    protected void addParameter(Parameter<?> p) {
        parameters.add(p);
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public abstract void render(net.minecraft.client.gui.GuiGraphics graphics, float partialTicks);
}
