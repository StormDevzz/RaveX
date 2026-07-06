package ravex.addon;

import java.io.File;

public class AddonUtil {
    public static boolean checkSignature(File file) {
        return file.exists() && file.length() > 0;
    }
}
