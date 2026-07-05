package ravex.modules.misc;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.StringParameter;
import ravex.parameter.NumberParameter;
import ravex.manager.LuaManager;
import org.luaj.vm2.LuaValue;
import java.util.List;
public class Spammer extends Module {
    public static final Spammer INSTANCE = new Spammer();
    public final ModeParameter mode = new ModeParameter("Mode", "Text", List.of("Text", "File"));
    public final StringParameter text = new StringParameter("Text", "RaveX on top!");
    public final StringParameter filePath = new StringParameter("File", "spam.txt");
    public final NumberParameter delay = new NumberParameter("Delay(ms)", 1000.0, 100.0, 10000.0, 100.0);
    private long lastSpamTime = 0;

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();
        if (now - lastSpamTime >= delay.getValue().longValue()) {
            lastSpamTime = now;
            try {
                LuaValue spamFn = LuaManager.INSTANCE.getGlobals().get("spammerTick");
                if (spamFn != null && !spamFn.isnil()) {
                    spamFn.call(
                        LuaValue.valueOf(mode.getValue()),
                        LuaValue.valueOf(text.getValue()),
                        LuaValue.valueOf(filePath.getValue())
                    );
                }
            } catch (Exception e) {
                ravex.RaveX.LOGGER.error("[Lua-Spammer] Error: " + e.getMessage());
            }
        }
    }
}
