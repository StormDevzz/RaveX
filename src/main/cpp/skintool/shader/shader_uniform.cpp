#include "../include/shader.hpp"

void setProj(GLuint prog, const float* m) {
    glUniformMatrix4fv(glGetUniformLocation(prog, "uProj"), 1, GL_FALSE, m);
}

void setView(GLuint prog, const float* m) {
    glUniformMatrix4fv(glGetUniformLocation(prog, "uView"), 1, GL_FALSE, m);
}

void setModel(GLuint prog, const float* m) {
    glUniformMatrix4fv(glGetUniformLocation(prog, "uModel"), 1, GL_FALSE, m);
}

void setLightDir(GLuint prog, float x, float y, float z) {
    float d[] = {x, y, z};
    glUniform3fv(glGetUniformLocation(prog, "uLightDir"), 1, d);
}

void setWireframe(GLuint prog, bool on) {
    glUniform1i(glGetUniformLocation(prog, "uWireframe"), on ? 1 : 0);
}

void setTextureUnit(GLuint prog, int unit) {
    glUniform1i(glGetUniformLocation(prog, "uTex"), unit);
}
