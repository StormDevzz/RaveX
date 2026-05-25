package volthack.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Model.class)
public class MixinModel {
    @ModifyVariable(
        method = "renderToBuffer",
        at = @At("HEAD"),
        argsOnly = true
    )
    private VertexConsumer modifyVertexConsumer(VertexConsumer original) {
        if (volthack.modules.render.Chams.INSTANCE.getEnabled()) {
            if (volthack.modules.render.Chams.INSTANCE.getRenderingPlayer() && volthack.modules.render.Chams.INSTANCE.getPlayers()) {
                return new volthack.util.render.ColoredVertexConsumer(original, () -> 
                    volthack.modules.render.Chams.INSTANCE.getChamsColor(0xFFFFFFFF)
                );
            }
            if (volthack.modules.render.Chams.INSTANCE.getRenderingCrystal() && volthack.modules.render.Chams.INSTANCE.getCrystals()) {
                return new volthack.util.render.ColoredVertexConsumer(original, () -> 
                    volthack.modules.render.Chams.INSTANCE.getChamsColor(0xFFFFFFFF)
                );
            }
        }
        return original;
    }

    @Inject(method = "renderToBuffer", at = @At("HEAD"))
    private void onRenderHead(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, int k, CallbackInfo ci) {
        if (volthack.modules.render.Chams.INSTANCE.getEnabled()) {
            if ((volthack.modules.render.Chams.INSTANCE.getRenderingPlayer() && volthack.modules.render.Chams.INSTANCE.getPlayers()) ||
                (volthack.modules.render.Chams.INSTANCE.getRenderingCrystal() && volthack.modules.render.Chams.INSTANCE.getCrystals())) {
                disableDepth();
            }
        }
    }

    @Inject(method = "renderToBuffer", at = @At("RETURN"))
    private void onRenderReturn(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, int k, CallbackInfo ci) {
        if (volthack.modules.render.Chams.INSTANCE.getEnabled()) {
            if ((volthack.modules.render.Chams.INSTANCE.getRenderingPlayer() && volthack.modules.render.Chams.INSTANCE.getPlayers()) ||
                (volthack.modules.render.Chams.INSTANCE.getRenderingCrystal() && volthack.modules.render.Chams.INSTANCE.getCrystals())) {
                enableDepth();
            }
        }
    }

    private static void disableDepth() {
        try {
            java.lang.reflect.Method m = com.mojang.blaze3d.systems.RenderSystem.class.getMethod("disableDepthTest");
            m.invoke(null);
        } catch (Exception e) {
            try {
                java.lang.reflect.Method m = com.mojang.blaze3d.systems.RenderSystem.class.getMethod("depthFunc", int.class);
                m.invoke(null, 519);
            } catch (Exception e2) {
                // Ignore
            }
        }
    }

    private static void enableDepth() {
        try {
            java.lang.reflect.Method m = com.mojang.blaze3d.systems.RenderSystem.class.getMethod("enableDepthTest");
            m.invoke(null);
        } catch (Exception e) {
            try {
                java.lang.reflect.Method m = com.mojang.blaze3d.systems.RenderSystem.class.getMethod("depthFunc", int.class);
                m.invoke(null, 515);
            } catch (Exception e2) {
                // Ignore
            }
        }
    }
}
