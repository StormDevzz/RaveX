#ifndef RAVEX_HAND_PHYSICS_H
#define RAVEX_HAND_PHYSICS_H

#include "shader_types.hpp"

namespace ravex::shaders::hand {

struct SpringState {
    Vec3 position;
    Vec3 velocity;
    Vec3 target;
    float stiffness;
    float damping;
    float mass;
};

struct InertiaState {
    Vec3 position;
    Vec3 velocity;
    Vec3 acceleration;
    float friction;
    float maxSpeed;
};

void initPhysics();
void updateSpring(float deltaTime);
void updateInertia(float deltaTime);
void setSpringTarget(const Vec3& target);
void setSpringProperties(float stiffness, float damping, float mass);
void applyForce(const Vec3& force);
void applyImpulse(const Vec3& impulse);

Vec3 getSpringPosition();
Vec3 getSpringVelocity();
Vec3 getInertiaPosition();
Vec3 getInertiaVelocity();

void resetPhysics();

float smoothDamp(float current, float target, float& velocity, float smoothTime, float deltaTime);

} // namespace ravex::shaders::hand

#endif
