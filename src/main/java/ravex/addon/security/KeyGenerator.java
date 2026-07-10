package ravex.addon.security;

import java.io.*;
import java.nio.file.*;
import java.security.*;

public class KeyGenerator {
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    public static void main(String[] args) throws Exception {
        Path outputDir = args.length > 0 ? Paths.get(args[0]) : Paths.get(".");
        Path publicKeyPath = outputDir.resolve("public.der");
        Path privateKeyPath = outputDir.resolve("private.der");

        KeyPairGenerator gen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        gen.initialize(KEY_SIZE);
        KeyPair pair = gen.generateKeyPair();

        Files.write(publicKeyPath, pair.getPublic().getEncoded());
        Files.write(privateKeyPath, pair.getPrivate().getEncoded());

        System.out.println("Generated RSA-2048 keypair:");
        System.out.println("  Public:  " + publicKeyPath.toAbsolutePath());
        System.out.println("  Private: " + privateKeyPath.toAbsolutePath());
        System.out.println("");
        System.out.println("To sign an addon:");
        System.out.println("  java " + AddonSigner.class.getName() + " " + privateKeyPath + " <addon-file>");
    }
}
