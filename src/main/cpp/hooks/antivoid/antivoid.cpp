#include "antivoid.h"

bool isVoidFall(double playerY, double motionY,
                int worldMinY, double fallDistance) {
    return playerY < (double)worldMinY && motionY < 0.0 && fallDistance > 0.0;
}
