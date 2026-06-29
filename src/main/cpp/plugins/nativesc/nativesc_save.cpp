#include "include/nativesc.hpp"
#include <cstdio>
#include <ctime>
#include <sstream>

namespace ravex {
namespace nativesc {

std::string generateFilename(const std::string& prefix, const std::string& ext) {
    std::time_t t = std::time(nullptr);
    char buf[64];
    std::strftime(buf, sizeof(buf), "%Y%m%d_%H%M%S", std::localtime(&t));
    return prefix + "_" + buf + "." + ext;
}

bool saveScreenshot(const std::string& directory) {
    CaptureResult result = captureScreen(CaptureSource::FullScreen);
    if (!result.success) return false;

    std::string filename = generateFilename("screenshot", "png");
    std::string fullPath = directory + "/" + filename;
    return saveToFile(result, fullPath);
}

bool saveScreenshotRegion(const CaptureRegion& region, const std::string& path) {
    CaptureResult result = captureRegion(region);
    if (!result.success) return false;
    return saveToFile(result, path);
}

}
}
