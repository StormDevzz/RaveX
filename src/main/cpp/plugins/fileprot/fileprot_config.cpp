#include "include/fileprot_config.hpp"
#include <fstream>
#include <sstream>

namespace ravex {
namespace fileprot {

FileProtConfig defaultConfig() {
    FileProtConfig cfg;
    cfg.databasePath = "fileprot.db";
    cfg.backupDir = "fileprot_backups";
    cfg.watchRecursive = 1;
    cfg.maxBackupVersions = 5;
    cfg.scanIntervalSec = 300;
    cfg.autoRepair = false;
    cfg.notifyOnChange = true;
    return cfg;
}

FileProtConfig loadConfig(const std::string& path) {
    FileProtConfig cfg = defaultConfig();
    std::ifstream file(path);
    if (!file.is_open()) return cfg;

    std::string line;
    while (std::getline(file, line)) {
        if (line.empty() || line[0] == ';' || line[0] == '#') continue;
        size_t eq = line.find('=');
        if (eq == std::string::npos) continue;

        std::string key = line.substr(0, eq);
        std::string val = line.substr(eq + 1);

        if (key == "database_path") cfg.databasePath = val;
        else if (key == "backup_dir") cfg.backupDir = val;
        else if (key == "watch_recursive") cfg.watchRecursive = std::stoi(val);
        else if (key == "max_backup_versions") cfg.maxBackupVersions = std::stoi(val);
        else if (key == "scan_interval_sec") cfg.scanIntervalSec = std::stoi(val);
        else if (key == "auto_repair") cfg.autoRepair = (val == "true" || val == "1");
        else if (key == "notify_on_change") cfg.notifyOnChange = (val == "true" || val == "1");
        else if (key == "watch_dir") cfg.watchDirs.push_back(val);
        else if (key == "exclude") cfg.excludePatterns.push_back(val);
        else if (key == "include_ext") cfg.includeExtensions.push_back(val);
    }
    return cfg;
}

bool saveConfig(const std::string& path, const FileProtConfig& config) {
    std::ofstream file(path);
    if (!file.is_open()) return false;

    file << "database_path=" << config.databasePath << "\n";
    file << "backup_dir=" << config.backupDir << "\n";
    file << "watch_recursive=" << config.watchRecursive << "\n";
    file << "max_backup_versions=" << config.maxBackupVersions << "\n";
    file << "scan_interval_sec=" << config.scanIntervalSec << "\n";
    file << "auto_repair=" << (config.autoRepair ? "true" : "false") << "\n";
    file << "notify_on_change=" << (config.notifyOnChange ? "true" : "false") << "\n";

    for (const auto& dir : config.watchDirs) {
        file << "watch_dir=" << dir << "\n";
    }
    for (const auto& pat : config.excludePatterns) {
        file << "exclude=" << pat << "\n";
    }
    for (const auto& ext : config.includeExtensions) {
        file << "include_ext=" << ext << "\n";
    }
    return file.good();
}

bool validateConfig(const FileProtConfig& config, std::string& error) {
    if (config.databasePath.empty()) {
        error = "database_path is empty";
        return false;
    }
    if (config.scanIntervalSec < 1) {
        error = "scan_interval_sec must be >= 1";
        return false;
    }
    if (config.maxBackupVersions < 1) {
        error = "max_backup_versions must be >= 1";
        return false;
    }
    return true;
}

}
}
