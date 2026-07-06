#ifndef RAVEX_SHADER_TRANSFORM_H
#define RAVEX_SHADER_TRANSFORM_H

#include "shader_types.hpp"

namespace ravex::shaders {

struct Transform {
    Vec3 position;
    Vec3 rotation;
    Vec3 scale;
    Matrix4x4 localMatrix;
    Matrix4x4 worldMatrix;

    Transform();
    void setPosition(float x, float y, float z);
    void setRotation(float pitch, float yaw, float roll);
    void setScale(float x, float y, float z);
    void rebuildLocalMatrix();
    void rebuildWorldMatrix(const Matrix4x4& parent);
    Vec3 transformPoint(const Vec3& point) const;
    Vec3 transformDirection(const Vec3& dir) const;
};

struct HandTransform {
    Transform handLocal;
    Matrix4x4 inverseViewRotation;
    Vec3 anchorOffset;

    void bindToHand(const Vec3& handPos, const Matrix4x4& handMatrix, const Matrix4x4& viewMatrix);
    Vec3 localizePoint(const Vec3& worldPoint) const;
    Vec3 worldifyPoint(const Vec3& localPoint) const;
};

} // namespace ravex::shaders

#endif
