package ravex.modules;

import ravex.parameter.Parameter;
import java.util.ArrayList;
import java.util.List;

public abstract class HudModule {
    private final String name;
    private boolean enabled;
    private int x;
    private int y;
    private int width;
    private int height;
    private final List<Parameter<?>> parameters = new ArrayList<>();

    public HudModule(String name, int defaultX, int defaultY, int width, int height) {
        this.name = name;
        this.x = defaultX;
        this.y = defaultY;
        this.width = width;
        this.height = height;
        this.enabled = true; // Enabled by default
    }

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
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
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
