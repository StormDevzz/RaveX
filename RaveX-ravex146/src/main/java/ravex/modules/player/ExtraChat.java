package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;

public class ExtraChat extends Module {
    public static final ExtraChat INSTANCE = new ExtraChat();

    public final BooleanParameter zov = new BooleanParameter("ZoV", false);
    public final NumberParameter chatHistorySize = new NumberParameter("Chat History", 1000.0, 100.0, 10000.0, 100.0);

    private ExtraChat() {
        super("ExtraChat", Category.PLAYER);
        addParameter(zov);
        addParameter(chatHistorySize);
    }
}
