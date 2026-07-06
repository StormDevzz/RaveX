#include "nofall.hpp"

bool calculateNoFall(
    const std::string& mode,
    double fallDistance,
    double currentY,
    bool currentOnGround,
    bool& outOnGround,
    double& outY
) {
    outOnGround = currentOnGround;
    outY = currentY;

    if (fallDistance <= 2.0) {
        return false;
    }

    if (mode == "Vanilla") {
        outOnGround = true;
        return true;
    } else if (mode == "NCP") {
        outOnGround = true;
        return true;
    } else if (mode == "Grim") {
        outOnGround = true;
        outY = currentY + 0.0001;
        return true;
    }

    return false;
}
