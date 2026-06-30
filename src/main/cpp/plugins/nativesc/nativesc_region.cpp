#include "include/nativesc.hpp"
#ifdef _WIN32
#include <windows.h>
#endif

namespace ravex {
namespace nativesc {

CaptureResult captureMonitor(int monitorId) {
    std::vector<std::string> monitors = listMonitors();
    if (monitorId < 0 || monitorId >= static_cast<int>(monitors.size())) {
        CaptureResult err;
        err.success = false;
        err.errorMsg = "Invalid monitor ID";
        return err;
    }

#ifdef _WIN32
    DISPLAY_DEVICE dd;
    dd.cb = sizeof(dd);
    if (!EnumDisplayDevices(nullptr, monitorId, &dd, 0)) {
        CaptureResult err;
        err.success = false;
        err.errorMsg = "Monitor not found";
        return err;
    }

    DEVMODE dm;
    dm.dmSize = sizeof(dm);
    EnumDisplaySettings(dd.DeviceName, ENUM_CURRENT_SETTINGS, &dm);

    CaptureRegion region;
    region.x = dm.dmPosition.x;
    region.y = dm.dmPosition.y;
    region.width = dm.dmPelsWidth;
    region.height = dm.dmPelsHeight;

    return captureRegion(region);
#else
    (void)monitorId;
    return captureScreen(CaptureSource::FullScreen);
#endif
}

CaptureRegion getPrimaryMonitorBounds() {
    CaptureRegion region;
#ifdef _WIN32
    region.x = 0;
    region.y = 0;
    region.width = GetSystemMetrics(SM_CXSCREEN);
    region.height = GetSystemMetrics(SM_CYSCREEN);
#else
    region.x = 0;
    region.y = 0;
    region.width = 1920;
    region.height = 1080;
#endif
    return region;
}

}
}
