package ravex.mcwrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MinecraftWrapper {
    private final Minecraft mc;

    public MinecraftWrapper() {
        this.mc = Minecraft.getInstance();
    }

    public Minecraft getRaw() { return mc; }

    public boolean isAvailable() {
        return mc != null;
    }

    public LocalPlayer getPlayer() {
        return mc.player;
    }

    public ClientLevel getLevel() {
        return mc.level;
    }

    public Level getRawLevel() {
        return mc.level;
    }

    public boolean hasWorld() {
        return mc.level != null;
    }

    public boolean hasPlayer() {
        return mc.player != null;
    }

    public boolean isInGame() {
        return mc.player != null && mc.level != null;
    }

    public Vec3 getPlayerPosition() {
        return mc.player != null ? mc.player.position() : Vec3.ZERO;
    }

    public double getPlayerX() { return mc.player != null ? mc.player.getX() : 0; }
    public double getPlayerY() { return mc.player != null ? mc.player.getY() : 0; }
    public double getPlayerZ() { return mc.player != null ? mc.player.getZ() : 0; }

    public float getPlayerYaw() { return mc.player != null ? mc.player.getYRot() : 0; }
    public float getPlayerPitch() { return mc.player != null ? mc.player.getXRot() : 0; }

    public int getScreenWidth() {
        return mc.getWindow() != null ? mc.getWindow().getGuiScaledWidth() : 0;
    }

    public int getScreenHeight() {
        return mc.getWindow() != null ? mc.getWindow().getGuiScaledHeight() : 0;
    }

    public boolean isOnSameThread() {
        return mc.isSameThread();
    }

    public void execute(Runnable runnable) {
        mc.execute(runnable);
    }

    public void setScreen(net.minecraft.client.gui.screens.Screen screen) {
        mc.setScreen(screen);
    }

    public net.minecraft.client.gui.screens.Screen getCurrentScreen() {
        return mc.screen;
    }

    public boolean isScreenOpened() {
        return mc.screen != null;
    }

    public Object getPlayerInput() {
        return mc.player != null ? mc.player.input : null;
    }

    public net.minecraft.client.Options getOptions() {
        return mc.options;
    }

    public net.minecraft.client.gui.Font getFont() {
        return mc.font;
    }

    public net.minecraft.client.renderer.entity.ItemRenderer getItemRenderer() {
        return mc.getItemRenderer();
    }

    public net.minecraft.client.renderer.texture.TextureManager getTextureManager() {
        return mc.getTextureManager();
    }

    public Object getSoundManager() {
        return mc.getSoundManager();
    }

    public net.minecraft.client.multiplayer.ClientPacketListener getConnection() {
        return mc.getConnection();
    }

    public long getWindowHandle() {
        return mc.getWindow() != null ? mc.getWindow().handle() : 0;
    }

    public int getFps() {
        return mc.getFps();
    }

    public boolean isInSingleplayer() {
        return mc.isLocalServer();
    }

    public boolean isInMultiplayer() {
        return mc.getConnection() != null && !mc.isLocalServer();
    }

    public Object getSingleplayerServer() {
        return mc.getSingleplayerServer();
    }
}
