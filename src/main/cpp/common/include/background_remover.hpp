#ifndef BACKGROUND_REMOVER_H
#define BACKGROUND_REMOVER_H

#include <vector>
#include <cstdint>

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
    bool isABGR = true
);

}
}

#endif
