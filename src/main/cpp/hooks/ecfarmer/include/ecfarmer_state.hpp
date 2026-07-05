#pragma once
#include "ecfarmer_types.hpp"

struct FarmerContext {
    ECFarmerState state;
    int targetX;
    int targetY;
    int targetZ;
    unsigned long breakStartTime;
    unsigned long lastActionTime;
    int prevSlot;
};
