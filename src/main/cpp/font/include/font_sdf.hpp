#pragma once
#include <cstdint>
#include <vector>

namespace ravex {
namespace font {

struct BakedAtlas {
    int width = 0;
    int height = 0;
    int pixelHeight = 0;
    std::vector<unsigned char> rgba;
    std::vector<int> metrics;
};

bool bakeAtlas(const unsigned char* ttfData, int ttfSize, int pixelHeight, BakedAtlas& out);

}
}
