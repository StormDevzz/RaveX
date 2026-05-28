#include "camera_correction.h"
#include "freecam.h"
#include "freecam_math.h"

namespace ravex::hooks::freecam {

    CameraCoordinates getCorrectedCoordinates(double partialTicks) {
        CameraCoordinates corrected;

        // Perform linear interpolation based on rendering-level partial ticks
        corrected.renderX = lerp(g_state.prevX, g_state.x, partialTicks);
        corrected.renderY = lerp(g_state.prevY, g_state.y, partialTicks);
        corrected.renderZ = lerp(g_state.prevZ, g_state.z, partialTicks);

        // Interpolate yaw rotation while safely handling wraps (e.g. crossing -180/180 degrees)
        float diffYaw = g_state.yaw - g_state.prevYaw;
        float wrappedDiffYaw = normalizeAngle(diffYaw);
        corrected.renderYaw = g_state.prevYaw + wrappedDiffYaw * static_cast<float>(partialTicks);

        // Interpolate pitch angle
        float diffPitch = g_state.pitch - g_state.prevPitch;
        corrected.renderPitch = g_state.prevPitch + diffPitch * static_cast<float>(partialTicks);

        return corrected;
    }

} // namespace ravex::hooks::freecam
