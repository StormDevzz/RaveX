#include "freecam.h"
#include <cmath>
#include <algorithm>

namespace ravex::hooks::freecam {

    FreeCamState g_state;

    void reset(double startX, double startY, double startZ, float startYaw, float startPitch) {
        g_state.prevX = g_state.x = startX;
        g_state.prevY = g_state.y = startY;
        g_state.prevZ = g_state.z = startZ;
        g_state.prevYaw = g_state.yaw = startYaw;
        g_state.prevPitch = g_state.pitch = startPitch;

        g_state.targetX = startX;
        g_state.targetY = startY;
        g_state.targetZ = startZ;
    }

    void turn(double yRot, double xRot) {
        // Direct angle adjustments, using vanilla 0.15D multiplier for perfect mouse sensitivity feeling
        g_state.yaw += static_cast<float>(yRot * 0.15);
        g_state.pitch += static_cast<float>(xRot * 0.15);
        
        // Clamp pitch to prevent going upside down
        g_state.pitch = std::max(-90.0f, std::min(90.0f, g_state.pitch));
    }

    void updatePosition(bool keyUp, bool keyDown, bool keyLeft, bool keyRight, bool keyJump, bool keyShift, double speed, double smoothness) {
        // Keep track of the last tick's position and rotation for smooth interpolation
        g_state.prevX = g_state.x;
        g_state.prevY = g_state.y;
        g_state.prevZ = g_state.z;
        g_state.prevYaw = g_state.yaw;
        g_state.prevPitch = g_state.pitch;

        float f = g_state.yaw * (3.14159265358979323846f / 180.0f);
        double sinYaw = std::sin(f);
        double cosYaw = std::cos(f);

        double dx = 0.0;
        double dy = 0.0;
        double dz = 0.0;

        if (keyUp) {
            dx -= sinYaw * speed;
            dz += cosYaw * speed;
        }
        if (keyDown) {
            dx += sinYaw * speed;
            dz -= cosYaw * speed;
        }
        if (keyLeft) {
            dx -= cosYaw * speed;
            dz -= sinYaw * speed;
        }
        if (keyRight) {
            dx += cosYaw * speed;
            dz += sinYaw * speed;
        }
        if (keyJump) {
            dy += speed;
        }
        if (keyShift) {
            dy -= speed;
        }

        g_state.targetX += dx;
        g_state.targetY += dy;
        g_state.targetZ += dz;

        double factor = 1.0 - smoothness;
        g_state.x += (g_state.targetX - g_state.x) * factor;
        g_state.y += (g_state.targetY - g_state.y) * factor;
        g_state.z += (g_state.targetZ - g_state.z) * factor;
    }

} // namespace ravex::hooks::freecam
