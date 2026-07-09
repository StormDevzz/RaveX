package ravex;

import net.fabricmc.api.ModInitializer;
import ravex.utility.sound.SoundUtility;


public class RaveXCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        SoundUtility.register();
    }
}
