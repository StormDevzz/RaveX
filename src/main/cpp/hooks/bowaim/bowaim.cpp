#include "bowaim.h"
#include <cmath>
#include <algorithm>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

namespace ravex {

BowAimResult solveBowAim(
    double playerX, double playerY, double playerZ,
    double targetX, double targetY, double targetZ,
    double targetVelX, double targetVelY, double targetVelZ,
    double targetHeight,
    double arrowSpeed
) {
    BowAimResult result;
    result.hit = false;
    result.yaw = 0;
    result.pitch = 0;
    result.ticks = 0;

    double best_diff = 9999.0;
    double best_yaw = 0;
    double best_pitch = 0;
    int best_ticks = 0;

    double d = 0.99; // drag
    double g = 0.05; // gravity

    double targetCenterY = targetY + targetHeight * 0.5;

    for (int t = 1; t <= 40; t++) {
        double predX = targetX + targetVelX * t;
        double predY = targetCenterY + targetVelY * t;
        double predZ = targetZ + targetVelZ * t;

        double dx = predX - playerX;
        double dy = predY - playerY;
        double dz = predZ - playerZ;

        double R = std::sqrt(dx*dx + dz*dz);
        double Y = dy;

        double St = 0.0;
        double term = 1.0;
        for (int i = 0; i < t; i++) {
            St += term;
            term *= d;
        }

        double Gt = 0.0;
        for (int i = 1; i < t; i++) {
            double sum_d = 0.0;
            double term_d = 1.0;
            for (int j = 0; j < i; j++) {
                sum_d += term_d;
                term_d *= d;
            }
            Gt += sum_d;
        }

        double v_h = R / St;
        double v_y = (Y + g * Gt) / St;

        double V_req = std::sqrt(v_h*v_h + v_y*v_y);
        double diff = std::abs(V_req - arrowSpeed);

        if (diff < best_diff) {
            best_diff = diff;
            best_ticks = t;

            double yaw_deg = std::atan2(dz, dx) * 180.0 / M_PI - 90.0;
            double pitch_deg = -std::atan2(v_y, v_h) * 180.0 / M_PI;

            best_yaw = yaw_deg;
            best_pitch = pitch_deg;
        }

        if (diff < 0.3) {
            break;
        }
    }

    result.hit = true;
    result.yaw = best_yaw;
    result.pitch = best_pitch;
    result.ticks = best_ticks;

    return result;
}

} // namespace ravex
