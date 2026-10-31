package ravex.mixin.render;

import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import ravex.modules.Modules;
import ravex.modules.misc.MoreChatHistory;

// raises vanilla's hardcoded 100 message chat history/visible line caps.
@Mixin(ChatComponent.class)
public class MixinChatHistory {

    @ModifyConstant(method = "addMessageToQueue", constant = @Constant(intValue = 100))
    private int ravex$historyLimit(int original) {
        return ravex$limit(original);
    }

    @ModifyConstant(method = "addMessageToDisplayQueue", constant = @Constant(intValue = 100))
    private int ravex$visibleLimit(int original) {
        return ravex$limit(original);
    }

    private static int ravex$limit(int original) {
        MoreChatHistory m = Modules.get(MoreChatHistory.class);
        if (m != null && Modules.enabled(MoreChatHistory.class)) {
            return Math.max(100, Math.min(1000, (int) m.limit));
        }
        return original;
    }
}
