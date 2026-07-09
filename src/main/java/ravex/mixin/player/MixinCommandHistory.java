package ravex.mixin.player;

import net.minecraft.client.CommandHistory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
<<<<<<< HEAD
import ravex.modules.misc.ChatHelper;
=======
import ravex.modules.misc.ChatUtils;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

@Mixin(CommandHistory.class)
public class MixinCommandHistory {

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 50))
    private int modifyInitCapacity(int original) {
<<<<<<< HEAD
        if (ChatHelper.maybeEnabled()) {
            return ChatHelper.itz().chatHistorySize.getValue().intValue();
=======
        if (ChatUtils.INSTANCE.getEnabled()) {
            return ChatUtils.INSTANCE.chatHistorySize.getValue().intValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
        return original;
    }

    @ModifyConstant(method = "addCommand", constant = @Constant(intValue = 50))
    private int modifyMaxHistory(int original) {
<<<<<<< HEAD
        if (ChatHelper.maybeEnabled()) {
            return ChatHelper.itz().chatHistorySize.getValue().intValue();
=======
        if (ChatUtils.INSTANCE.getEnabled()) {
            return ChatUtils.INSTANCE.chatHistorySize.getValue().intValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
        return original;
    }
}
