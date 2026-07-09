#include "../include/skin_texture.hpp"

void SkinTexture::bind(int unit) const {
    glActiveTexture(GL_TEXTURE0 + unit);
    glBindTexture(GL_TEXTURE_2D, id);
}

void SkinTexture::destroy() {
    if (id) {
        glDeleteTextures(1, &id);
        id = 0;
    }
}
