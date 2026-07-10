#include "include/shieldfucker_platform.hpp"

namespace shieldfucker {

struct ShieldBreakState {
    int targetId = -1;
    uint64_t lastBreakTime = 0;
    uint64_t lastSwitchTime = 0;
    int breakCount = 0;
};

static ShieldBreakState state;

void trackerReset() {
    state = ShieldBreakState{};
}

void trackerSetTarget(int entityId) {
    if (state.targetId != entityId) {
        state.targetId = entityId;
        state.breakCount = 0;
        state.lastBreakTime = 0;
    }
}

bool trackerCanBreak(uint64_t delayMs) {
    uint64_t now = currentTimeMs();
    if (now - state.lastBreakTime >= delayMs) {
        state.lastBreakTime = now;
        state.breakCount++;
        return true;
    }
    return false;
}

bool trackerCanSwitch(uint64_t delayMs) {
    uint64_t now = currentTimeMs();
    if (now - state.lastSwitchTime >= delayMs) {
        state.lastSwitchTime = now;
        return true;
    }
    return false;
}

int trackerGetBreakCount() {
    return state.breakCount;
}

}
