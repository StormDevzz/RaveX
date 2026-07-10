package ravex.addon.security;

import ravex.addon.util.AddonException;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;

public class AddonSigner {
    private static final String KEY_ALGORITHM = "RSA";

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: AddonSigner <private-key.der> <addon-file> [addon-file...]");
            System.err.println("");
            System.err.println("Signs addon files with the given RSA private key.");
            System.err.println("Produces <addon-file>.ravex-sig sidecar files.");
            System.err.println("");
            System.err.println("To generate a keypair:");
            System.err.println("  keytool -genkeypair -alias ravex -keyalg RSA -keysize 2048");
            System.err.println("          -dname CN=RaveX -keystore ravex.jks -storepass changeit");
            System.err.println("  keytool -exportcert -alias ravex -keystore ravex.jks -storepass changeit");
            System.err.println("          -file public.der");
            System.err.println("  keytool -importkeystore -srckeystore ravex.jks -storepass changeit");
            System.err.println("          -destkeystore private.p12 -deststoretype PKCS12 -deststorepass changeit");
            System.err.println("  openssl pkcs12 -in private.p12 -nodes -nocerts -out private.pem");
            System.err.println("  openssl pkcs8 -topk8 -inform PEM -outform DER -in private.pem");
            System.err.println("          -out private.der -nocrypt");
            System.exit(1);
        }

        try {
            Path keyPath = Paths.get(args[0]);
            byte[] keyBytes = Files.readAllBytes(keyPath);
            KeyFactory kf = KeyFactory.getInstance(KEY_ALGORITHM);
            PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

            int signed = 0;
            for (int i = 1; i < args.length; i++) {
                Path addonFile = Paths.get(args[i]);
                if (!Files.exists(addonFile)) {
                    System.err.println("SKIP (not found): " + addonFile);
                    continue;
                }
                AddonSignature.sign(addonFile, privateKey);
                System.out.println("SIGNED: " + addonFile + " -> " + addonFile.getFileName() + ".ravex-sig");
                signed++;
            }
            System.out.println("Done. Signed " + signed + " file(s).");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
