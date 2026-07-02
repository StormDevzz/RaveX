#pragma once

#include <string>
#include <cmath>

enum class ElytraMode {
    Vanilla,
    Control,
    Grim,
    NCP,
    Minemen,
    Packet,
    Boost,
    TickShift
};

struct VelocityInput {
    double hSpeed;
    double vSpeed;
    double glide;
    double yaw;
    double pitch;
    bool jump;
    bool sneak;
};

struct VelocityOutput {
    double x;
    double y;
    double z;
};

struct MotionState {
    double x;
    double y;
    double z;
    double yaw;
    double pitch;
    bool jump;
    bool sneak;
    bool onGround;
};

struct BypassConfig {
    double friction;
    double maxYDrop;
    double minYLimit;
    bool clampVertical;
    bool enableHorizontalFriction;
};

inline ElytraMode modeFromString(const std::string& mode) {
    if (mode == "Vanilla")  return ElytraMode::Vanilla;
    if (mode == "Control")  return ElytraMode::Control;
    if (mode == "Grim")     return ElytraMode::Grim;
    if (mode == "NCP")      return ElytraMode::NCP;
    if (mode == "Minemen")  return ElytraMode::Minemen;
    if (mode == "Packet")   return ElytraMode::Packet;
    if (mode == "Boost")    return ElytraMode::Boost;
    if (mode == "TickShift") return ElytraMode::TickShift;
    return ElytraMode::Vanilla;
}

inline double degToRad(double degrees) {
    return degrees * (M_PI / 180.0);
}
