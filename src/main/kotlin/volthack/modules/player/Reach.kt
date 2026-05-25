package volthack.modules.player

import volthack.setting.Category
import volthack.setting.Module

object Reach : Module("Reach", "Increases your block break/build and entity attack interaction ranges", Category.PLAYER) {
    val blockReach by float("Block Reach", 6.0f, 3.0f, 10.0f, 0.1f)
    val entityReach by float("Entity Reach", 5.0f, 3.0f, 10.0f, 0.1f)
}
