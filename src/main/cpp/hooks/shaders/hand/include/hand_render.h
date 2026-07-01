#ifndef RAVEX_HAND_RENDER_H
#define RAVEX_HAND_RENDER_H

#include "shader_types.h"
#include "shader_config.h"
#include "shader_pipeline.h"

namespace ravex::shaders::hand {

struct HandRenderInput {
    Vertex* vertices;
    int vertexCount;
    ShaderConfig config;
    Matrix4x4 modelMatrix;
    Matrix4x4 viewMatrix;
    Matrix4x4 projectionMatrix;
    Vec3 cameraPos;
    Vec3 lightDir;
    Vec3 handPos;
    float time;
    float deltaTime;
};

void initHandSystem();
void shutdownHandSystem();
void updateHandSystem(float deltaTime);
void renderHand(const HandRenderInput& input);
void resetHandSystem();

ShaderPipeline& getPipeline();

} // namespace ravex::shaders::hand

#endif
