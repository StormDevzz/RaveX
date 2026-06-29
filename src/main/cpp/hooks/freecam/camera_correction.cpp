#include "camera_correction.h"
#include "freecam.h"
#include "freecam_math.h"

namespace ravex::hooks::freecam {

    CameraCoordinates getCorrectedCoordinates(double partialTicks) {
        CameraCoordinates corrected;

        
        corrected.renderX = lerp(g_state.prevX, g_state.x, partialTicks);
        corrected.renderY = lerp(g_state.prevY, g_state.y, partialTicks);
        corrected.renderZ = lerp(g_state.prevZ, g_state.z, partialTicks);

        
        float diffYaw = g_state.yaw - g_state.prevYaw;
        float wrappedDiffYaw = normalizeAngle(diffYaw);
        corrected.renderYaw = g_state.prevYaw + wrappedDiffYaw * static_cast<float>(partialTicks);

        
        float diffPitch = g_state.pitch - g_state.prevPitch;
        corrected.renderPitch = g_state.prevPitch + diffPitch * static_cast<float>(partialTicks);

        return corrected;
    }

} 
