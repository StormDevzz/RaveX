package ravex.addon.security;

import ravex.addon.util.AddonException;
import java.io.*;
import java.nio.file.*;
import java.security.*;

public class AddonSignature {
    private static final String KEY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String KEY_RESOURCE = "/assets/ravex/security/public.der";
    private static final String SIG_EXTENSION = ".ravex-sig";

    private static PublicKey cachedPublicKey;

    public static synchronized PublicKey getPublicKey() throws AddonException {
        if (cachedPublicKey != null) return cachedPublicKey;

        try (InputStream in = AddonSignature.class.getResourceAsStream(KEY_RESOURCE)) {
            if (in == null)
                throw new AddonException("Public key not found: " + KEY_RESOURCE);
            byte[] encoded = in.readAllBytes();
            KeyFactory kf = KeyFactory.getInstance(KEY_ALGORITHM);
            cachedPublicKey = kf.generatePublic(new java.security.spec.X509EncodedKeySpec(encoded));
            return cachedPublicKey;
        } catch (Exception e) {
            throw new AddonException("Failed to load public key", e);
        }
    }

    public static boolean isSigned(Path addonFile) {
        Path sigFile = addonFile.resolveSibling(addonFile.getFileName() + SIG_EXTENSION);
        return Files.exists(sigFile) && Files.isReadable(sigFile);
    }

    public static void requireSignature(Path addonFile) throws AddonException {
        if (!isSigned(addonFile))
            throw new AddonException("Addon not signed: " + addonFile.getFileName() +
                " (missing " + addonFile.getFileName() + SIG_EXTENSION + ")");
        if (!verify(addonFile))
            throw new AddonException("Addon signature invalid: " + addonFile.getFileName());
    }

    public static boolean verify(Path addonFile) {
        try {
            PublicKey key = getPublicKey();
            Path sigFile = addonFile.resolveSibling(addonFile.getFileName() + SIG_EXTENSION);

            byte[] fileData = Files.readAllBytes(addonFile);
            byte[] sigBytes = Files.readAllBytes(sigFile);

            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initVerify(key);
            sig.update(fileData);
            return sig.verify(sigBytes);
        } catch (Exception e) {
            return false;
        }
    }

    public static void sign(Path addonFile, PrivateKey privateKey) throws AddonException {
        try {
            byte[] fileData = Files.readAllBytes(addonFile);
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initSign(privateKey);
            sig.update(fileData);
            byte[] signature = sig.sign();

            Path sigFile = addonFile.resolveSibling(addonFile.getFileName() + SIG_EXTENSION);
            Files.write(sigFile, signature);
        } catch (Exception e) {
            throw new AddonException("Failed to sign addon: " + addonFile.getFileName(), e);
        }
    }
}
