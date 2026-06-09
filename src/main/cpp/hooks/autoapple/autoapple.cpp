#include "autoapple.h"

bool calculateShouldEat(
    double health,
    double absorption,
    double healthThreshold
) {
    double totalHealth = health + absorption;

    if (totalHealth <= healthThreshold) {
        return true;
    }

    if (totalHealth <= 6.0) {
        return true;
    }

    return false;
}
