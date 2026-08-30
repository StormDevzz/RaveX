#pragma once
#include <functional>
#include <string>
#include <windows.h>

namespace ravex::game {

struct LaunchParams {
    std::string username;
    std::string uuid;
    std::string accessToken;
    std::string mcVersion;
    std::wstring gameDir;
    int ramMb = 4096;
    std::string jvmArgs;
    std::string loader;
    std::wstring javaExe;
    bool offline = false;
    std::string assetIndexId;
};

struct GameProcessImpl;
struct GameProcess {
    GameProcessImpl* impl = nullptr;
    bool valid() const;
    bool isRunning() const;
    void kill();
    void close();
    DWORD getExitCode() const;
};

bool launchMinecraft(const LaunchParams& params, std::string* error,
                     const std::function<void(const std::string&)>& consoleLine,
                     GameProcess& outProcess);

int requiredJavaVersion(const std::string& mcVersion);

}
