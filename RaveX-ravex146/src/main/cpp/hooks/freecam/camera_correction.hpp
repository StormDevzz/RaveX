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


    CameraCoordinates getCorrectedCoordinates(double partialTicks);

}

#endif
