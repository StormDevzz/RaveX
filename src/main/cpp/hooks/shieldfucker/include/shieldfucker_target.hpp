#pragma once

struct TargetInfo {
    int entityId = -1;
    double x = 0.0;
    double y = 0.0;
    double z = 0.0;
    double health = 0.0;
    bool hasShield = false;
    bool isBlocking = false;
    bool isPlayer = false;
    double distance = 0.0;
    bool hasLineOfSight = false;
};

struct BreakAction {
    int targetId = -1;
    float yaw = 0.0f;
    float pitch = 0.0f;
    bool shouldBreak = false;
    bool shouldSwitch = false;
    int switchSlot = -1;
};
