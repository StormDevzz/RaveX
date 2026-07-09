#pragma once
#include <GL/glew.h>
#include <string>

struct SkinTexture {
    GLuint id = 0;
    int width = 0, height = 0;

    bool load(const std::string& path);
    void bind(int unit = 0) const;
    void destroy();

    int getWidth() const { return width; }
    int getHeight() const { return height; }
};
