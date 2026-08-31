#pragma once
#include <cstdint>
#include <string>
#include <vector>

namespace ravex {

struct Account {
    std::string name;
    std::string uuid;
    std::string accessToken;
    std::string type;
};

struct InstanceCfg {
    std::string name;
    std::string mcVersion;
    std::string loader;
    int ramMb = 4096;
    std::string jvmArgs;
    std::string notes;
    std::string javaPath;
    std::string loaderVersion;
    bool useBundledJava = true;
    bool offlineMode = false;
    std::string assetIndexId;
};

struct LauncherConfig {
    std::string javaPath;
    bool checkUpdatesOnStart = true;
    bool showSnapshots = false;
    bool showBeta = false;
    bool showAlpha = false;
    std::string theme = "dark";
    std::string language = "en-us";
    uint32_t customBg = 0x1C1C1E;
    uint32_t customPanel = 0x282828;
    uint32_t customText = 0xE1E1E1;
    uint32_t customAccent = 0x5A8CFF;
    uint32_t customButton = 0x5A8CFF;
    uint32_t customGlow = 0xC8E1FF;
    bool glowEnabled = true;
    bool customDarkIcons = false;
    int customAlpha = 255;
    std::vector<std::string> installHistory;
    bool autoStart = false;
    std::vector<Account> accounts;
    int activeAccount = -1;
    bool saveLogs = true;
    bool telemetryEnabled = true;
};

LauncherConfig loadLauncherConfig();
void saveLauncherConfig(const LauncherConfig& config);
std::vector<InstanceCfg> listInstances();
InstanceCfg loadInstance(const std::wstring& dir);
void saveInstance(const std::wstring& dir, const InstanceCfg& cfg);
void deleteInstance(const std::wstring& dir);

}