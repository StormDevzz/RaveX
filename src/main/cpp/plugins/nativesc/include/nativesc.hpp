#pragma once
#include <string>
#include <vector>
#include <cstdint>

namespace ravex {
namespace nativesc {

enum class ImageFormat {
    PNG,
    BMP,
    JPEG
};

enum class CaptureSource {
    FullScreen,
    ActiveWindow,
    Region
};

struct CaptureRegion {
    int x;
    int y;
    int width;
    int height;
};

struct CaptureResult {
    bool success;
    std::vector<unsigned char> data;
    int width;
    int height;
    int channels;
    std::string errorMsg;
};

bool initialize();
void shutdown();
bool encodePng(const std::vector<unsigned char>& raw, int w, int h, int channels,
               std::vector<unsigned char>& out);
CaptureResult captureScreen(CaptureSource source, ImageFormat fmt = ImageFormat::PNG);
CaptureResult captureRegion(const CaptureRegion& region, ImageFormat fmt = ImageFormat::PNG);
bool saveToFile(const CaptureResult& result, const std::string& path);
std::vector<std::string> listMonitors();

}
}
