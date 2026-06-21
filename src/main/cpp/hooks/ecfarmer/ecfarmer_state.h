#pragma once
#include "include/ecfarmer_types.h"

struct FarmerContext {
    ECFarmerState state;
    int targetX;
    int targetY;
    int targetZ;
    unsigned long breakStartTime;
    unsigned long lastActionTime;
    int prevSlot;
};
