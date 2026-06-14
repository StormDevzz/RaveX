#ifndef BACKGROUND_REMOVER_H
#define BACKGROUND_REMOVER_H

#include <vector>
#include <cstdint>

namespace ravex {
namespace utility {

/**
 * Removes the background color from a raw ARGB/ABGR pixel buffer by making matching pixels transparent.
 * @param pixels Pointer to the raw pixel buffer (each pixel is a 32-bit integer).
 * @param width Width of the image in pixels.
 * @param height Height of the image in pixels.
 * @param keyRed Red component of the key color (0-255).
 * @param keyGreen Green component of the key color (0-255).
 * @param keyBlue Blue component of the key color (0-255).
 * @param threshold Color distance threshold (0-255).
 * @param isABGR If true, the pixels are in ABGR format (0xAABBGGRR). Otherwise ARGB (0xAARRGGBB).
 */
void removeBackground(
    uint32_t* pixels,
    int width,
    int height,
    uint8_t keyRed,
    uint8_t keyGreen,
    uint8_t keyBlue,
    int threshold,
    bool isABGR = true
);

} // namespace utility
} // namespace ravex

#endif // BACKGROUND_REMOVER_H
