package ravex.mixin.font;

import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import org.lwjgl.util.freetype.FT_Face;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TrueTypeGlyphProviderDefinition.class)
public class MixinTrueTypeGlyphProviderDefinition {

    @Redirect(
        method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;)Lcom/mojang/blaze3d/font/GlyphProvider;",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/freetype/FreeType;FT_Get_Font_Format(Lorg/lwjgl/util/freetype/FT_Face;)Ljava/lang/String;")
    )
    private String overrideFontFormat(FT_Face face) {
        String format = org.lwjgl.util.freetype.FreeType.FT_Get_Font_Format(face);
        if ("TrueType".equals(format) || "CFF".equals(format) || "Type 1".equals(format)) {
            return "TrueType";
        }
        return format;
    }
}
