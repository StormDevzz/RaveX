#pragma once
#include <GL/glew.h>

struct SkinModelPart {
    GLuint vao = 0;
    GLuint vbo = 0;
    int count = 0;
};

struct SkinModel {
    SkinModelPart head, body, leftArm, rightArm, leftLeg, rightLeg;

    bool build();
    void draw(GLuint prog, bool wireframe) const;
    void destroy();
};
