package ravex.mcwrapper;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MinecraftWrapper {
    private final Minecraft mc;

    private static MinecraftWrapper wrapperInstance;

    public static MinecraftWrapper getWrapper() {
        if (wrapperInstance == null) {
            wrapperInstance = new MinecraftWrapper();
        }
        return wrapperInstance;
    }

    public static Minecraft getInstance() {
        return Minecraft.getInstance();
    }

    public MinecraftWrapper() {
        this.mc = Minecraft.getInstance();
    }

    @Nullable
    public GameModeWrapper getGameMode() {
        return mc.gameMode != null ? new GameModeWrapper(mc.gameMode) : null;
    }

    public Minecraft getRaw() { return mc; }

    public boolean isAvailable() {
        return mc != null;
    }

    @Nullable
    public LocalPlayer getPlayer() {
        return mc.player;
    }

    @Nullable
    public ClientLevel getLevel() {
        return mc.level;
    }

    @Nullable
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

    @Nullable
    public net.minecraft.client.gui.screens.Screen getCurrentScreen() {
        return mc.screen;
    }

    public boolean isScreenOpened() {
        return mc.screen != null;
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

    @Nullable
    public net.minecraft.client.multiplayer.ClientPacketListener getConnection() {
        return mc.getConnection();
    }

    public Window getWindow() {
        return mc.getWindow();
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

    @Nullable
    public MinecraftServer getSingleplayerServer() {
        return mc.getSingleplayerServer();
    }

    @Nullable
    public net.minecraft.world.phys.HitResult getHitResult() {
        return mc.hitResult;
    }

    public net.minecraft.client.renderer.LevelRenderer getLevelRenderer() {
        return mc.levelRenderer;
    }

    @Nullable
    public Entity getCrosshairPickEntity() {
        return mc.crosshairPickEntity;
    }

    public net.minecraft.client.renderer.GameRenderer getGameRenderer() {
        return mc.gameRenderer;
    }

    @Nullable
    public net.minecraft.client.multiplayer.ServerData getCurrentServer() {
        return mc.getCurrentServer();
    }

    public double getPlayerFallDistance() {
        return mc.player != null ? mc.player.fallDistance : 0.0;
    }

    public boolean isPlayerOnGround() {
        return mc.player != null && mc.player.onGround();
    }

    public boolean isPlayerHorizontalCollision() {
        return mc.player != null && mc.player.horizontalCollision;
    }

    @Nullable
    public Vec3 getPlayerDeltaMovement() {
        return mc.player != null ? mc.player.getDeltaMovement() : null;
    }

    public void setPlayerDeltaMovement(Vec3 motion) {
        if (mc.player != null) mc.player.setDeltaMovement(motion);
    }

    public void setPlayerDeltaMovement(double x, double y, double z) {
        if (mc.player != null) mc.player.setDeltaMovement(x, y, z);
    }

    public Vec3 getPlayerEyePosition() {
        return mc.player != null ? mc.player.getEyePosition(1.0F) : Vec3.ZERO;
    }

    public Vec3 getPlayerViewVector() {
        return mc.player != null ? mc.player.getViewVector(1.0F) : Vec3.ZERO;
    }

    public int getPlayerFoodLevel() {
        return mc.player != null ? mc.player.getFoodData().getFoodLevel() : 20;
    }

    public float getPlayerHealth() {
        return mc.player != null ? mc.player.getHealth() : 0;
    }

    @Nullable
    public net.minecraft.world.entity.player.Abilities getPlayerAbilities() {
        return mc.player != null ? mc.player.getAbilities() : null;
    }

    public boolean isKeyDown(int key) {
        return mc.options.keyMappings[key].isDown();
    }

    public boolean isJumpKeyDown() {
        return mc.options.keyJump.isDown();
    }

    public boolean isForwardKeyDown() {
        return mc.options.keyUp.isDown();
    }

    public boolean isBackKeyDown() {
        return mc.options.keyDown.isDown();
    }

    public boolean isLeftKeyDown() {
        return mc.options.keyLeft.isDown();
    }

    public boolean isRightKeyDown() {
        return mc.options.keyRight.isDown();
    }

    public boolean isSneakKeyDown() {
        return mc.options.keyShift.isDown();
    }

    public boolean isUseKeyDown() {
        return mc.options.keyUse.isDown();
    }

    public boolean isAttackKeyDown() {
        return mc.options.keyAttack.isDown();
    }

    @Nullable
    public Object getPlayerInput() {
        return mc.player != null ? mc.player.input : null;
    }

    public Vec2 getPlayerMovementInput() {
        if (mc.player == null || mc.player.input == null) return Vec2.ZERO;
        return mc.player.input.getMoveVector();
    }
}
