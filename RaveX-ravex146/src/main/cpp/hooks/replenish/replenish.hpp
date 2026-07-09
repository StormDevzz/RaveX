#pragma once

#include <vector>
#include <string>

struct InventorySlot {
    int slot;
    std::string itemId;
    int count;
};

struct ReplenishResult {
    int hotbarSlot;
    int inventorySlot;
    int needed;
    int available;
};

std::vector<ReplenishResult> findReplenishTargets(
    const std::vector<InventorySlot>& hotbarSlots,
    const std::vector<InventorySlot>& inventorySlots,
    int threshold
);
