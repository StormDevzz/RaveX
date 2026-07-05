#include "velocity_math.hpp"

LookVector calculateLookVector(double yaw, double pitch) {
    double yawRad = degToRad(yaw);
    double pitchRad = degToRad(pitch);
    return {
        -std::sin(yawRad) * std::cos(pitchRad),
        -std::sin(pitchRad),
        std::cos(yawRad) * std::cos(pitchRad)
    };
}

VelocityOutput calculateControlVelocity(const VelocityInput& in) {
    double yawRad = degToRad(in.yaw);
    VelocityOutput out{};
    out.x = -std::sin(yawRad) * in.hSpeed;
    out.y = in.jump ? in.vSpeed : (in.sneak ? -in.vSpeed : -in.glide);
    out.z = std::cos(yawRad) * in.hSpeed;
    return out;
}

VelocityOutput calculateVanillaVelocity(const VelocityInput& in) {
    LookVector look = calculateLookVector(in.yaw, in.pitch);
    VelocityOutput out;
    out.x = look.x * in.hSpeed;
    out.y = look.y * in.hSpeed;
    out.z = look.z * in.hSpeed;

    if (in.jump) {
        out.y = look.y * in.vSpeed;
    } else if (in.sneak) {
        out.y = -in.vSpeed;
    } else {
        out.y = -in.glide;
    }
    return out;
}

VelocityOutput calculateGrimVelocity(const VelocityInput& in) {
    LookVector look = calculateLookVector(in.yaw, in.pitch);
    VelocityOutput out;
    double hFactor = 0.85;
    double vFactor = 0.8;
    out.x = look.x * in.hSpeed * hFactor;
    out.y = look.y * in.hSpeed * vFactor;
    out.z = look.z * in.hSpeed * hFactor;

    if (in.jump) {
        out.y = look.y * in.vSpeed * vFactor;
    } else if (in.sneak) {
        out.y = -in.vSpeed * vFactor;
    } else {
        out.y = -in.glide * 0.5;
    }
    return out;
}

VelocityOutput calculateNCPVelocity(const VelocityInput& in) {
    LookVector look = calculateLookVector(in.yaw, in.pitch);
    VelocityOutput out;
    double factor = 0.9;
    out.x = look.x * in.hSpeed * factor;
    out.y = look.y * in.hSpeed * factor;
    out.z = look.z * in.hSpeed * factor;

    if (in.jump) {
        out.y = look.y * in.vSpeed * factor;
    } else if (in.sneak) {
        out.y = -in.vSpeed * factor;
    } else {
        out.y = -in.glide * 0.8;
    }
    return out;
}

VelocityOutput calculateMinemenVelocity(const VelocityInput& in) {
    LookVector look = calculateLookVector(in.yaw, in.pitch);
    VelocityOutput out;
    double factor = 0.75;
    out.x = look.x * in.hSpeed * factor;
    out.y = look.y * in.hSpeed * factor * 0.5;
    out.z = look.z * in.hSpeed * factor;

    if (in.jump) {
        out.y = look.y * in.vSpeed * 0.6;
    } else if (in.sneak) {
        out.y = -in.vSpeed * 0.6;
    } else {
        out.y = -in.glide * 0.3;
    }
    return out;
}

VelocityOutput calculatePacketVelocity(const VelocityInput& in) {
    double yawRad = degToRad(in.yaw);
    double forward = in.jump ? 1.0 : (in.sneak ? -1.0 : 0.0);

    VelocityOutput out;
    out.x = -std::sin(yawRad) * in.hSpeed * forward;
    out.y = in.jump ? in.vSpeed : (in.sneak ? -in.vSpeed : -in.glide);
    out.z = std::cos(yawRad) * in.hSpeed * forward;
    return out;
}

VelocityOutput calculateBoostVelocity(const VelocityInput& in) {
    LookVector look = calculateLookVector(in.yaw, in.pitch);
    VelocityOutput out;
    out.x = look.x * in.hSpeed;
    out.y = look.y * in.hSpeed;
    out.z = look.z * in.hSpeed;

    if (!in.jump && !in.sneak) {
        out.y = -in.glide;
    }
    return out;
}

VelocityOutput calculateTickShiftVelocity(const VelocityInput& in) {
    LookVector look = calculateLookVector(in.yaw, in.pitch);
    VelocityOutput out;
    out.x = look.x * in.hSpeed * 0.5;
    out.y = look.y * in.hSpeed * 0.3;
    out.z = look.z * in.hSpeed * 0.5;

    if (in.jump) {
        out.y = look.y * in.vSpeed * 0.5;
    } else if (in.sneak) {
        out.y = -in.vSpeed * 0.5;
    } else {
        out.y = -in.glide * 0.5;
    }
    return out;
}

VelocityOutput dispatchVelocityCalculation(ElytraMode mode, const VelocityInput& in) {
    switch (mode) {
        case ElytraMode::Control:   return calculateControlVelocity(in);
        case ElytraMode::Vanilla:   return calculateVanillaVelocity(in);
        case ElytraMode::Grim:      return calculateGrimVelocity(in);
        case ElytraMode::NCP:       return calculateNCPVelocity(in);
        case ElytraMode::Minemen:   return calculateMinemenVelocity(in);
        case ElytraMode::Packet:    return calculatePacketVelocity(in);
        case ElytraMode::Boost:     return calculateBoostVelocity(in);
        case ElytraMode::TickShift: return calculateTickShiftVelocity(in);
    }
    return calculateVanillaVelocity(in);
}
