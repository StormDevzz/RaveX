#pragma once
#include <string>

bool calculateNoFall(
    const std::string& mode,
    double fallDistance,
    double currentY,
    bool currentOnGround,
    bool& outOnGround,
    double& outY
);
