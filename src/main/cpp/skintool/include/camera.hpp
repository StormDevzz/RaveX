#pragma once

struct Camera {
    float distance = 5.f;
    float yaw = 30.f;
    float pitch = -20.f;
    float cx = 0.f, cy = 12.f, cz = 0.f;

    void orbit(float dyaw, float dpitch);
    void zoom(float d);
    void viewMatrix(float* out) const;
    void reset();
};
