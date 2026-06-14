#include "replenish.h"

std::vector<ReplenishResult> findReplenishTargets(
    const std::vector<InventorySlot>& hotbarSlots,
    const std::vector<InventorySlot>& inventorySlots,
    int threshold
) {
    std::vector<ReplenishResult> results;

    for (const auto& hb : hotbarSlots) {
        if (hb.count >= threshold || hb.itemId.empty()) {
            continue;
        }

        int needed = threshold - hb.count;
        if (needed <= 0) continue;

        for (const auto& inv : inventorySlots) {
            if (inv.itemId.empty()) continue;
            if (inv.itemId != hb.itemId) continue;
            if (inv.count <= 0) continue;

            int available = inv.count;
            if (available > needed) {
                available = needed;
            }

            ReplenishResult result;
            result.hotbarSlot = hb.slot;
            result.inventorySlot = inv.slot;
            result.needed = needed;
            result.available = available;
            results.push_back(result);
            break;
        }
    }

    return results;
}
