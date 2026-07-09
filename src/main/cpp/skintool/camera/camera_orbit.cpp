#include "../include/camera.hpp"
#include <algorithm>

void Camera::orbit(float dyaw, float dpitch) {
    yaw += dyaw;
    pitch += dpitch;
    pitch = std::clamp(pitch, -89.f, 89.f);
}

void Camera::zoom(float d) {
    distance = std::clamp(distance + d, 1.5f, 20.f);
}
