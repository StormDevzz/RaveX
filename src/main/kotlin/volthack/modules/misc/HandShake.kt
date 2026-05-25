package volthack.modules.misc

import volthack.setting.Category
import volthack.setting.Module

object HandShake : Module("HandShake", "Allows spoofing your client brand name", Category.MISC) {
    private val modeSettingObj = mode("Brand Mode", listOf("Lunar", "Vanilla", "Fabric", "Custom"), "Lunar", "The client brand to send to the server")
    val modeSetting by modeSettingObj

    private val customBrandSettingObj = text("Custom Brand", "Lunar 1.21.11", "Custom brand name used when Brand Mode is Custom")
    val customBrandSetting by customBrandSettingObj

    init {
        customBrandSettingObj.showIf { modeSetting == "Custom" }
    }

    fun getBrandName(): String {
        if (!enabled) {
            return "fabric"
        }
        return when (modeSetting) {
            "Lunar" -> "Lunar 1.21.11"
            "Vanilla" -> "vanilla"
            "Fabric" -> "fabric"
            "Custom" -> customBrandSetting
            else -> "fabric"
        }
    }
}
