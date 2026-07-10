#ifndef RAVEX_HAND_TRANSFORM_H
#define RAVEX_HAND_TRANSFORM_H

#include "shader_types.hpp"
#include "shader_transform.hpp"

namespace ravex::shaders::hand {

void initHandTransform();
void setHandSmoothTime(float time);
void updateHandTransform(const Vec3& rawHandPos, const Matrix4x4& handMatrix,
                          const Matrix4x4& viewMatrix, float deltaTime);
void applyCameraInverse(const Matrix4x4& viewMatrix);

Vec3 localizePoint(const Vec3& worldPoint);
Vec3 worldifyPoint(const Vec3& localPoint);

const HandTransform& getHandTransform();
Vec3 getSmoothedPosition();

}

#endif
