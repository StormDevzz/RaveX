package volthack.modules.render

import volthack.setting.Category
import volthack.setting.Module

object NoRender : Module("NoRender", "Disables rendering of specific visual overlay elements", Category.RENDER) {
    val guiBackground by boolean("Gui Background", true, "Disable dark background when inside container GUIs")
    val waterOverlay by boolean("Water Overlay", true, "Disable blue overlay when underwater")
    val fireOverlay by boolean("Fire Overlay", true, "Disable fire overlay when burning")
    val weather by boolean("Weather", false, "Disable weather rendering")
    val worldBorder by boolean("World Border", false, "Disable world border rendering")
    val breakParticles by boolean("Break Particles", false, "Disable block breaking/destruction particles")
    val scoreboard by boolean("Scoreboard", false, "Disable scoreboard rendering")
}
