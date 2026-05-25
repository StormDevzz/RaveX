package volthack.modules.render

import volthack.setting.Category
import volthack.setting.Module

object WorldTweaks : Module("WorldTweaks", "Customizes fog and environment rendering", Category.RENDER) {
    val color by color("Fog Color", 0xFF6C63FF.toInt(), "The color of the world fog")
}
