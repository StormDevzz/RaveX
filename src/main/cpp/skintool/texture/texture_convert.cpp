#include "../include/skin_texture.hpp"
#include <cstdio>
#include <vector>


static unsigned char* makeCheckerboard(int& w, int& h) {
    w = 64; h = 64;
    auto* data = new unsigned char[w * h * 4];
    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            bool white = ((x / 8) + (y / 8)) % 2 == 0;
            int c = white ? 220 : 180;
            int idx = (y * w + x) * 4;
            data[idx] = c;
            data[idx+1] = c;
            data[idx+2] = c;
            data[idx+3] = 255;
        }
    }
    return data;
}


bool createFallbackTexture(SkinTexture& tex) {
    if (tex.id) return true;
    int w, h;
    unsigned char* data = makeCheckerboard(w, h);

    glGenTextures(1, &tex.id);
    glBindTexture(GL_TEXTURE_2D, tex.id);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
    delete[] data;
    tex.width = w;
    tex.height = h;
    return true;
}
