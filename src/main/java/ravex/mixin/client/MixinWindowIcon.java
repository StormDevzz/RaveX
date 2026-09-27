package ravex.mixin.client;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.RaveX;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Mixin(Minecraft.class)
public class MixinWindowIcon {

    @Inject(method = "createTitle", at = @At("HEAD"), cancellable = true)
    private void onCreateTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Ravex " + RaveX.version + " - Minecraft 1.21.11");
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        long windowHandle = mc.getWindow().handle();

        String title = "Ravex " + RaveX.version + " - Minecraft 1.21.11";
        GLFW.glfwSetWindowTitle(windowHandle, title);

        try (InputStream is = getClass().getResourceAsStream("/assets/ravex/textures/ravexv2.png")) {
            if (is == null) return;

            BufferedImage original = ImageIO.read(is);
            if (original == null) return;

            int maxSize = 256;
            int w = original.getWidth();
            int h = original.getHeight();
            BufferedImage image = original;
            if (w > maxSize || h > maxSize) {
                double scale = Math.min((double) maxSize / w, (double) maxSize / h);
                int nw = Math.max(1, (int) Math.round(w * scale));
                int nh = Math.max(1, (int) Math.round(h * scale));
                BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(original, 0, 0, nw, nh, null);
                g.dispose();
                image = scaled;
                w = nw;
                h = nh;
            }

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
        } catch (Throwable ignored) {
        }
    }
}
