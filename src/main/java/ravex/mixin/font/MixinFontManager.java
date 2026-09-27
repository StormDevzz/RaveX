package ravex.mixin.font;

import com.mojang.blaze3d.font.GlyphProvider;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.mcwrapper.MinecraftWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(FontManager.class)
public class MixinFontManager {

    @Unique
    private static final Identifier RAVEX_INTER = Identifier.fromNamespaceAndPath("ravex", "inter");

    @Unique
    private static final Identifier RAVEX_INTER_BOLD = Identifier.fromNamespaceAndPath("ravex", "inter_bold");

    @Unique
    private static final List<FontSet> RAVEX_INJECTED_SETS = new ArrayList<>();

    @Unique
    private static final List<GlyphProvider> RAVEX_INJECTED_PROVIDERS = new ArrayList<>();

    @Inject(method = "apply", at = @At("TAIL"))
    private void onApplyTail(CallbackInfo ci) {
        FontManager self = (FontManager) (Object) this;
        Map<Identifier, FontSet> sets = ((FontManagerAccessor) self).ravexGetFontSets();
        for (FontSet old : RAVEX_INJECTED_SETS) {
            try {
                old.close();
            } catch (Exception ignored) {
            }
        }
        RAVEX_INJECTED_SETS.clear();
        for (GlyphProvider old : RAVEX_INJECTED_PROVIDERS) {
            try {
                if (old instanceof AutoCloseable closeable) closeable.close();
            } catch (Exception ignored) {
            }
        }
        RAVEX_INJECTED_PROVIDERS.clear();
        injectInterFont(sets, RAVEX_INTER, "inter.ttf");
        injectInterFont(sets, RAVEX_INTER_BOLD, "inter_bold.ttf");
    }

    @Unique
    private static void injectInterFont(Map<Identifier, FontSet> sets, Identifier id, String file) {
        if (sets.containsKey(id)) return;
        try {
            ResourceManager rm = MinecraftWrapper.getWrapper().getRaw().getResourceManager();
            TextureManager tm = MinecraftWrapper.getWrapper().getTextureManager();
            TrueTypeGlyphProviderDefinition def = new TrueTypeGlyphProviderDefinition(
                Identifier.fromNamespaceAndPath("ravex", file), 11.0f, 8.0f,
                TrueTypeGlyphProviderDefinition.Shift.NONE, "");
            var either = def.unpack();
            if (either.right().isPresent()) return;
            GlyphProviderDefinition.Loader loader = either.left().orElse(null);
            if (loader == null) return;
            GlyphProvider provider;
            try {
                provider = loader.load(rm);
            } catch (IOException e) {
                return;
            }
            if (provider == null) return;
            GlyphProvider.Conditional cond = new GlyphProvider.Conditional(provider, FontOption.Filter.ALWAYS_PASS);
            FontSet set = new FontSet(new GlyphStitcher(tm, id));
            set.reload(List.of(cond), Set.of());
            sets.put(id, set);
            RAVEX_INJECTED_SETS.add(set);
            RAVEX_INJECTED_PROVIDERS.add(provider);
        } catch (Throwable ignored) {
        }
    }
}
