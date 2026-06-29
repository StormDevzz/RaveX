#include "include/nativesc.hpp"
#include "include/nativesc_types.hpp"
#include <cstring>
#include <algorithm>

namespace ravex {
namespace nativesc {

class FrameBuffer {
public:
    FrameBuffer() : width(0), height(0) {}

    bool resize(int w, int h) {
        width = w;
        height = h;
        data.resize(w * h * 4);
        return true;
    }

    void clear() {
        std::memset(data.data(), 0, data.size());
    }

    void copyFrom(const unsigned char* src, int srcW, int srcH, int channels) {
        resize(srcW, srcH);
        for (int y = 0; y < srcH; y++) {
            for (int x = 0; x < srcW; x++) {
                int srcIdx = (y * srcW + x) * channels;
                int dstIdx = (y * width + x) * 4;
                data[dstIdx] = src[srcIdx];
                data[dstIdx + 1] = (channels > 1) ? src[srcIdx + 1] : 0;
                data[dstIdx + 2] = (channels > 2) ? src[srcIdx + 2] : 0;
                data[dstIdx + 3] = 255;
            }
        }
    }

    void flipVertical() {
        std::vector<unsigned char> row(width * 4);
        for (int y = 0; y < height / 2; y++) {
            int topY = y;
            int bottomY = height - 1 - y;
            std::memcpy(row.data(), &data[topY * width * 4], width * 4);
            std::memcpy(&data[topY * width * 4], &data[bottomY * width * 4], width * 4);
            std::memcpy(&data[bottomY * width * 4], row.data(), width * 4);
        }
    }

    const unsigned char* rawData() const { return data.data(); }
    int getWidth() const { return width; }
    int getHeight() const { return height; }
    size_t size() const { return data.size(); }

private:
    int width;
    int height;
    std::vector<unsigned char> data;
};

}
}
