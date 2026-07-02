#include "faststairs.hpp"

double calculateClimbSpeed(const std::string& mode, double currentY, double speedFactor) {
    double baseSpeed = (currentY > 0.0) ? currentY : 0.15;
    if (mode == "Boost") {
        return baseSpeed * speedFactor * 1.35;
    }
    
    return baseSpeed * speedFactor;
}
