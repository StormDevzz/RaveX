#include "background_remover.h"
#include <cmath>

namespace ravex {
namespace utility {

void removeBackground(
    uint32_t* pixels,
    int width,
    int height,
    uint8_t keyRed,
    uint8_t keyGreen,
    uint8_t keyBlue,
    int threshold,
    bool isABGR
) {
    if (!pixels || width <= 0 || height <= 0) return;

    int totalPixels = width * height;
    for (int i = 0; i < totalPixels; ++i) {
        uint32_t pixel = pixels[i];
        uint8_t r, g, b;

        if (isABGR) {
            r = pixel & 0xFF;
            g = (pixel >> 8) & 0xFF;
            b = (pixel >> 16) & 0xFF;
        } else {
            b = pixel & 0xFF;
            g = (pixel >> 8) & 0xFF;
            r = (pixel >> 16) & 0xFF;
        }

        // Euclidean distance in RGB color space
        double dist = std::sqrt(
            (r - keyRed) * (r - keyRed) +
            (g - keyGreen) * (g - keyGreen) +
            (b - keyBlue) * (b - keyBlue)
        );

        if (dist <= threshold) {
            // Set alpha to 0 (fully transparent)
            pixels[i] = pixel & 0x00FFFFFF;
        }
    }
}

} // namespace utility
} // namespace ravex
