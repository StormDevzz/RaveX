#pragma once

struct ShieldFuckerConfig {
    double range = 4.5;
    double wallRange = 3.0;
    double switchDelay = 100.0;
    double attackDelay = 200.0;
    double rotateSpeed = 180.0;
    bool throughWalls = true;
    bool autoSwitch = true;
    bool silentRotate = true;
    bool targetPlayers = true;
    bool targetMonsters = false;
    bool onlyAxe = true;
    int preferredSlot = -1;
};
