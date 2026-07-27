package ravex.modules.hud;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;

import ravex.modules.client.Hud;

@Module(name = "ChatHud", category = "HUD")
public class ChatHud extends ravex.modules.Module {
    public int x;
    public int y;
    public int width;
    public int height;
    @Parameter(name = "Scale", min = 0.25, max = 4.0, step = 0.05)
    public double scale = 1.0;
    private ChatHud() {
        super("ChatHud", 2, 30, 200, 100);
        this.x = 4; this.y = 4; this.width = 0; this.height = 0;
    }
    public void render(GuiGraphics graphics, float partialTicks) {
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