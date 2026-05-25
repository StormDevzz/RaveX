package volthack.modules.world

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.animal.Animal
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module
import volthack.util.world.breed.BreedUtils

object AutoBreed : Module("AutoBreed", "Automatically feeds and breeds nearby animals using correct foods from your hotbar", Category.WORLD) {
    private val reachRange by float("Range", 4.5f, 2.0f, 6.0f, 0.1f)
    private val feedDelay by int("Feed Delay Ticks", 4, 1, 20)

    private var tickCooldown = 0

    init {
        EventBus.listen<TickEvent> { onTick() }
    }

    private fun onTick() {
        if (!enabled) return

        if (tickCooldown > 0) {
            tickCooldown--
            return
        }

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val world = mc.level ?: return

        // 1. Scan for animals in range that can breed
        val animals = world.entitiesForRendering()
            .filterIsInstance<Animal>()
            .filter { player.distanceToSqr(it) <= (reachRange * reachRange).toDouble() }
            .filter { BreedUtils.canBreed(it) }

        for (animal in animals) {
            // 2. Find matching food in hotbar
            val foodSlot = BreedUtils.findFoodSlot(animal)
            if (foodSlot != -1) {
                // 3. Feed animal
                BreedUtils.feedAnimal(animal, foodSlot)
                tickCooldown = feedDelay
                break // Feed one animal per tick/delay interval
            }
        }
    }

    override fun onDisable() {
        tickCooldown = 0
    }
}
