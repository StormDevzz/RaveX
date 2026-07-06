package ravex.mixin.client;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.RaveX;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Mixin(Minecraft.class)
public class MixinWindowIcon {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        long windowHandle = mc.getWindow().handle();

        String title = "Ravex " + RaveX.version + " - Minecraft 1.21.11";
        GLFW.glfwSetWindowTitle(windowHandle, title);

        try (InputStream is = getClass().getResourceAsStream("/assets/ravex/textures/icon.png")) {
            if (is == null) return;

            BufferedImage image = ImageIO.read(is);
            if (image == null) return;

            int w = image.getWidth();
            int h = image.getHeight();

            ByteBuffer buffer = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
            int[] pixels = new int[w * h];
            image.getRGB(0, 0, w, h, pixels, 0, w);

            for (int pixel : pixels) {
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
            buffer.flip();

            GLFWImage icon = GLFWImage.malloc();
            icon.set(w, h, buffer);

            GLFWImage.Buffer iconBuffer = GLFWImage.malloc(1);
            iconBuffer.put(0, icon);
            GLFW.glfwSetWindowIcon(windowHandle, iconBuffer);

            iconBuffer.free();
            icon.free();
        } catch (Exception ignored) {
        }
    }
}
