#pragma once
#include <string>
#include <vector>

namespace ravex {
namespace fileprot {

struct FileProtConfig {
    std::string databasePath = "fileprot.db";
    std::string backupDir = "fileprot_backups";
    std::vector<std::string> watchDirs;
    std::vector<std::string> excludePatterns;
    std::vector<std::string> includeExtensions;
    int watchRecursive = 1;
    int maxBackupVersions = 5;
    int scanIntervalSec = 300;
    bool autoRepair = false;
    bool notifyOnChange = true;
};

FileProtConfig loadConfig(const std::string& path);
FileProtConfig defaultConfig();
bool saveConfig(const std::string& path, const FileProtConfig& config);
bool validateConfig(const FileProtConfig& config, std::string& error);

}
}
