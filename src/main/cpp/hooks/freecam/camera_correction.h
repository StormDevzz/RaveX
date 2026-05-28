#ifndef RAVEX_CAMERA_CORRECTION_H
#define RAVEX_CAMERA_CORRECTION_H

namespace ravex::hooks::freecam {

    struct CameraCoordinates {
        double renderX = 0.0;
        double renderY = 0.0;
        double renderZ = 0.0;
        float renderYaw = 0.0f;
        float renderPitch = 0.0f;
    };

    /**
     * Computes the corrected, smoothly-interpolated camera coordinates based on partial ticks
     * to eliminate jitter, bypass rendering clipping, and ensure stable rendering.
     */
    CameraCoordinates getCorrectedCoordinates(double partialTicks);

} // namespace ravex::hooks::freecam

#endif // RAVEX_CAMERA_CORRECTION_H
