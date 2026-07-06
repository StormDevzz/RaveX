package ravex.mixin.player;

import net.minecraft.client.CommandHistory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import ravex.modules.player.ExtraChat;

@Mixin(CommandHistory.class)
public class MixinCommandHistory {

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 50))
    private int modifyInitCapacity(int original) {
        if (ExtraChat.INSTANCE.getEnabled()) {
            return ExtraChat.INSTANCE.chatHistorySize.getValue().intValue();
        }
        return original;
    }

    @ModifyConstant(method = "addCommand", constant = @Constant(intValue = 50))
    private int modifyMaxHistory(int original) {
        if (ExtraChat.INSTANCE.getEnabled()) {
            return ExtraChat.INSTANCE.chatHistorySize.getValue().intValue();
        }
        return original;
    }
}
