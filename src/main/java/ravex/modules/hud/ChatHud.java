package ravex.modules.hud;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.gui.GuiGraphics;

import ravex.modules.client.Hud;

import ravex.parameter.NumberParameter;
@ModuleInfo(name = "ChatHud", category = "HUD")
public class ChatHud extends ravex.modules.Module {
    public int x;
    public int y;
    public int width;
    public int height;
public final NumberParameter scale = new NumberParameter("Scale", 1.0, 0.25, 4.0, 0.05);
    private ChatHud() {
        this.x = 4; this.y = 4; this.width = 0; this.height = 0;
    }
    public void render(GuiGraphics graphics, float partialTicks) {
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ChatHud").getEnabled();
    }

    public static ChatHud itz() {
        return ravex.manager.ModuleManager.delegate(ChatHud.class);
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
    

    @Override
    public int getX() { return x; }
    @Override
    public void setX(int x) { this.x = x; }
    @Override
    public int getY() { return y; }
    @Override
    public void setY(int y) { this.y = y; }
    @Override
    public int getWidth() { return width; }
    @Override
    public void setWidth(int w) { this.width = w; }
    @Override
    public int getHeight() { return height; }
    @Override
    public void setHeight(int h) { this.height = h; }

    public boolean isHud() {
        return hud;
    }
}