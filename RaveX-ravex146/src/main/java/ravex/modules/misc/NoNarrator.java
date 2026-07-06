package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.utility.misc.NativeLoader;

public class NoNarrator extends Module {
    public static final NoNarrator INSTANCE = new NoNarrator();

    private NoNarrator() {
        super("NoNarrator", Category.MISC);
    }

    static {
        NativeLoader.load();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        if (mc.options.narrator().get() != NarratorStatus.OFF) {
            mc.options.narrator().set(NarratorStatus.OFF);
        }

        try {
            nativeForceOff();
        } catch (UnsatisfiedLinkError ignored) {}
    }

    private native void nativeForceOff();
    private native boolean nativeIsForced();
}
