#pragma once
#include <GL/glew.h>


const char* vertSource();
const char* fragSource();


GLuint compileShader(GLenum type, const char* src);
GLuint makeProgram();


void setProj(GLuint prog, const float* m);
void setView(GLuint prog, const float* m);
void setModel(GLuint prog, const float* m);
void setLightDir(GLuint prog, float x, float y, float z);
void setWireframe(GLuint prog, bool on);
void setTextureUnit(GLuint prog, int unit);
