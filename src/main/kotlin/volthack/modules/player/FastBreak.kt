package volthack.modules.player

import volthack.setting.Category
import volthack.setting.Module

object FastBreak : Module("FastBreak", "Removes the vanilla 5-tick delay between breaking blocks legitimately to bypass anti-cheats", Category.PLAYER) {
    // FastBreak is a toggle module whose action is managed directly via MultiPlayerGameMode mixin.
}
