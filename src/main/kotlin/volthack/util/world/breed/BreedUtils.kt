package volthack.util.world.breed

import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.item.ItemStack

object BreedUtils {
    private val mc get() = Minecraft.getInstance()
    private val player get() = mc.player

    // 1. Check if stack is breeding food for the animal
    fun isBreedingFood(stack: ItemStack, animal: Animal): Boolean {
        if (stack.isEmpty) return false
        return animal.isFood(stack)
    }

    // 2. Find the slot of valid breeding food in player's hotbar
    fun findFoodSlot(animal: Animal): Int {
        val p = player ?: return -1
        for (i in 0..8) {
            if (isBreedingFood(p.inventory.getItem(i), animal)) {
                return i
            }
        }
        return -1
    }

    // 3. Check if the animal is ready/eligible to breed (adult and not in love mode already)
    fun canBreed(animal: Animal): Boolean {
        return animal.isAlive && animal.age == 0 && !animal.isInLove
    }

    // 4. Feed the animal using the food slot
    fun feedAnimal(animal: Animal, foodSlot: Int): Boolean {
        val p = player ?: return false
        val mc = Minecraft.getInstance()

        // Swap to food slot
        val prevSlot = p.inventory.selectedSlot
        p.inventory.selectedSlot = foodSlot

        // Interact with the animal using the hand
        mc.gameMode?.interact(p, animal, InteractionHand.MAIN_HAND)
        p.swing(InteractionHand.MAIN_HAND)

        // Restore slot
        p.inventory.selectedSlot = prevSlot
        return true
    }
}
