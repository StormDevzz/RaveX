#include "../include/camera.hpp"

void Camera::reset() {
    distance = 5.f;
    yaw = 30.f;
    pitch = -20.f;
    cx = 0.f; cy = 12.f; cz = 0.f;
}
