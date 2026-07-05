#include "ecfarmer.hpp"

bool canPlaceOn(int bx, int by, int bz, int belowBx, int belowBy, int belowBz) {
    return true;
}

int findPreferredPlaceSlot(int currentSlot, int ecSlot) {
    if (ecSlot >= 0 && ecSlot < 9) return ecSlot;
    return currentSlot;
}

bool isValidPlacePos(int bx, int by, int bz, int belowBx, int belowBy, int belowBz) {
    if (by != belowBy + 1) return false;
    if (bx != belowBx || bz != belowBz) return false;
    return true;
}
