package ravex.mixin.client;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@Mixin(VanillaPackResources.class)
public class MixinVanillaPackResources {

    @Inject(method = "getNamespaces", at = @At("RETURN"), cancellable = true)
    private void onGetNamespaces(PackType type, CallbackInfoReturnable<Set<String>> cir) {
        if (type == PackType.CLIENT_RESOURCES) {
            Set<String> namespaces = cir.getReturnValue();
            if (namespaces != null) {
                Set<String> newNamespaces = new HashSet<>(namespaces);
                newNamespaces.add("ravex");
                cir.setReturnValue(newNamespaces);
            }
        }
    }

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    private void onGetResource(PackType type, Identifier id, CallbackInfoReturnable<IoSupplier<InputStream>> cir) {
        if (type == PackType.CLIENT_RESOURCES && "ravex".equals(id.getNamespace())) {
            String path = "/assets/ravex/" + id.getPath();
            IoSupplier<InputStream> supplier = () -> {
                InputStream is = MixinVanillaPackResources.class.getResourceAsStream(path);
                if (is == null) {
                    throw new java.io.FileNotFoundException("Resource not found: " + path);
                }
                return is;
            };
            cir.setReturnValue(supplier);
        }
    }

    @Inject(method = "listResources", at = @At("HEAD"), cancellable = true)
    private void onListResources(PackType type, String namespace, String path, PackResources.ResourceOutput output, CallbackInfo ci) {
        if (type == PackType.CLIENT_RESOURCES && "ravex".equals(namespace)) {
            String[] assets = {
                "companion/textures/kotost.png",
                "companion/textures/ninja.png",
                "companion/textures/vanya.png",
                "lang/en_us.json",
                "lang/ru_ru.json",
                "natives/libravex_jni.so",
                "natives/libravex_loader.so",
                "shaders/poisoncloud.custom.frag",
                "shaders/custom.frag",
                "sounds.json",
                "sounds/disable.ogg",
                "sounds/enable.ogg",
                "sounds/gui_close.ogg",
                "sounds/gui_open.ogg",
                "sounds/settings_close.ogg",
                "sounds/settings_open.ogg",
                "textures/skull.png"
            };

            for (String asset : assets) {
                if (asset.startsWith(path)) {
                    Identifier id = Identifier.fromNamespaceAndPath(namespace, asset);
                    String resourcePath = "/assets/ravex/" + asset;
                    IoSupplier<InputStream> supplier = () -> {
                        InputStream is = MixinVanillaPackResources.class.getResourceAsStream(resourcePath);
                        if (is == null) {
                            throw new java.io.FileNotFoundException("Resource not found: " + resourcePath);
                        }
                        return is;
                    };
                    output.accept(id, supplier);
                }
            }
            ci.cancel();
        }
    }
}
