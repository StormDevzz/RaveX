package volthack.modules.player

import net.minecraft.client.Minecraft
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module

object Replenish : Module("Replenish", "Automatically refills item stacks in your hotbar from your inventory", Category.PLAYER) {
    private val threshold by int("Threshold", 8, 1, 64)
    private val delay by int("Delay Ticks", 4, 1, 20)

    private val hotbarCache = Array<Item>(9) { Items.AIR }
    private var tickDelay = 0

    init {
        EventBus.listen<TickEvent> { onTick() }
    }

    private fun onTick() {
        if (!enabled) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return

        if (tickDelay > 0) {
            tickDelay--
            return
        }

        val inv = player.inventory
        val menuId = player.containerMenu.containerId

        for (i in 0..8) {
            val stack = inv.getItem(i)
            val cachedItem = hotbarCache[i]

            if (stack.isEmpty) {
                // If it became empty, but we previously had an item there, try to refill it!
                if (cachedItem != Items.AIR) {
                    val invSlot = findItemInInventory(cachedItem)
                    if (invSlot != -1) {
                        // Swap inventory item to hotbar
                        mc.gameMode?.handleInventoryMouseClick(menuId, invSlot, i, ClickType.SWAP, player)
                        tickDelay = delay
                        break
                    } else {
                        // We ran out of this item entirely, reset cache
                        hotbarCache[i] = Items.AIR
                    }
                }
                continue
            }

            // Update cache with the current item
            hotbarCache[i] = stack.item

            // If stack is damageable or doesn't stack, skip
            if (!stack.isStackable || stack.maxStackSize <= 1) continue

            // If stack count is below threshold and we have room to refill it
            if (stack.count <= threshold && stack.count < stack.maxStackSize) {
                val invSlot = findItemInInventory(stack.item)
                if (invSlot != -1) {
                    // Quick move or swap to refill
                    mc.gameMode?.handleInventoryMouseClick(menuId, invSlot, i, ClickType.SWAP, player)
                    tickDelay = delay
                    break
                }
            }
        }
    }

    private fun findItemInInventory(item: Item): Int {
        val player = Minecraft.getInstance().player ?: return -1
        val inv = player.inventory
        // Inventory slots: 9..35 represent the upper main inventory
        for (i in 9..35) {
            val stack = inv.getItem(i)
            if (!stack.isEmpty && stack.item == item) {
                return i
            }
        }
        return -1
    }

    override fun onEnable() {
        val player = Minecraft.getInstance().player ?: return
        for (i in 0..8) {
            hotbarCache[i] = player.inventory.getItem(i).item
        }
        tickDelay = 0
    }
}
