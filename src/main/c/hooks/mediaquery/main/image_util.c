#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"
#include "image_util.h"
#include <stdlib.h>
#include <string.h>

uint8_t* decode_to_rgba(const uint8_t* data, size_t data_len, int* w, int* h) {
    if (!data || data_len == 0) return NULL;
    int n = 0;
    uint8_t* rgba = stbi_load_from_memory(data, (int)data_len, w, h, &n, STBI_rgb_alpha);
    return rgba;
}

uint8_t* resize_rgba(const uint8_t* rgba, int w, int h, int tw, int th) {
    if (!rgba || tw <= 0 || th <= 0) return NULL;
    uint8_t* out = (uint8_t*)calloc((size_t)tw * th, 4);
    if (!out) return NULL;
    for (int y = 0; y < th; y++) {
        for (int x = 0; x < tw; x++) {
            int sx = x * w / tw;
            int sy = y * h / th;
            if (sx >= w) sx = w - 1;
            if (sy >= h) sy = h - 1;
            int si = (sy * w + sx) * 4;
            int di = (y * tw + x) * 4;
            out[di + 0] = rgba[si + 0];
            out[di + 1] = rgba[si + 1];
            out[di + 2] = rgba[si + 2];
            out[di + 3] = rgba[si + 3];
        }
    }
    return out;
}

void free_image(uint8_t* pixels) {
    if (pixels) stbi_image_free(pixels);
}
