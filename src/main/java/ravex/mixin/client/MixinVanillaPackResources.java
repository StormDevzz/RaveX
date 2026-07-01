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

    private static boolean isValidTTF(java.io.File file) {
        if (file == null || !file.exists() || file.length() < 12) return false;
        try (java.io.DataInputStream dis = new java.io.DataInputStream(new java.io.FileInputStream(file))) {
            int magic = dis.readInt();
            return magic == 0x00010000 || magic == 0x74727565;
        } catch (Exception e) {
            return false;
        }
    }

    private static InputStream getResourceStream(String path, String idPath) throws java.io.IOException {
        InputStream is = MixinVanillaPackResources.class.getResourceAsStream(path);
        if (is != null) {
            return is;
        }

        java.io.File cacheFile = new java.io.File(System.getProperty("user.home"), ".ravex/" + idPath);
        if (cacheFile.exists() && cacheFile.length() > 0) {
            if (idPath.endsWith(".ttf") && !isValidTTF(cacheFile)) {
                cacheFile.delete();
            } else {
                return new java.io.FileInputStream(cacheFile);
            }
        }

        java.io.File parent = cacheFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        String remoteUrl = "https://raw.githubusercontent.com/StormDevzz/RaveX/main/assets/" + cacheFile.getName();
        try {
            java.net.URL url = new java.net.URL(remoteUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() == 200) {
                java.io.File tempDownload = new java.io.File(parent, cacheFile.getName() + ".tmp");
                try (InputStream inStream = conn.getInputStream();
                     java.io.FileOutputStream outStream = new java.io.FileOutputStream(tempDownload)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }
                }
                if (!tempDownload.renameTo(cacheFile)) {
                    java.nio.file.Files.copy(tempDownload.toPath(), cacheFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    tempDownload.delete();
                }
                return new java.io.FileInputStream(cacheFile);
            }
        } catch (Throwable ignored) {}

        throw new java.io.FileNotFoundException("Resource not found: " + path);
    }

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    private void onGetResource(PackType type, Identifier id, CallbackInfoReturnable<IoSupplier<InputStream>> cir) {
        if (type == PackType.CLIENT_RESOURCES && "ravex".equals(id.getNamespace())) {
            String path = "/assets/ravex/" + id.getPath();
            IoSupplier<InputStream> supplier = () -> getResourceStream(path, id.getPath());
            cir.setReturnValue(supplier);
        }
    }

    @Inject(method = "listResources", at = @At("HEAD"), cancellable = true)
    private void onListResources(PackType type, String namespace, String path, PackResources.ResourceOutput output, CallbackInfo ci) {
        if (type == PackType.CLIENT_RESOURCES && "ravex".equals(namespace)) {
            String[] assets = {
                "font/comfortaa.json",
                "font/comfortaa.ttf",
                "font/sf_bold.json",
                "font/sf_bold.ttf",
                "font/sf_medium.json",
                "font/sf_medium.ttf",
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
                    IoSupplier<InputStream> supplier = () -> getResourceStream(resourcePath, asset);
                    output.accept(id, supplier);
                }
            }
            ci.cancel();
        }
    }
}
