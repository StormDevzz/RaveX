#include "include/fileprot.hpp"
#include <fstream>
#include <algorithm>
#include <vector>

#ifdef _WIN32
#include <windows.h>
#else
#include <dirent.h>
#endif

namespace ravex {
namespace fileprot {

std::string findLatestBackup(const std::string& filePath, const std::string& backupDir) {
    size_t slashPos = filePath.find_last_of("/\\");
    std::string baseName = (slashPos != std::string::npos) ? filePath.substr(slashPos + 1) : filePath;

    std::vector<std::string> backups;

#ifdef _WIN32
    std::string pattern = backupDir + "\\" + baseName + ".*.bak";
    WIN32_FIND_DATA ffd;
    HANDLE hFind = FindFirstFile(pattern.c_str(), &ffd);
    if (hFind != INVALID_HANDLE_VALUE) {
        do {
            backups.push_back(ffd.cFileName);
        } while (FindNextFile(hFind, &ffd) != 0);
        FindClose(hFind);
    }
#else
    DIR* dp = opendir(backupDir.c_str());
    if (dp) {
        struct dirent* entry;
        std::string prefix = baseName + ".";
        while ((entry = readdir(dp)) != nullptr) {
            std::string name = entry->d_name;
            if (name.find(prefix) == 0 && name.size() > prefix.size() + 4 &&
                name.substr(name.size() - 4) == ".bak") {
                backups.push_back(name);
            }
        }
        closedir(dp);
    }
#endif

    if (backups.empty()) return "";

    std::sort(backups.begin(), backups.end());
    std::string latest = backups.back();
    return backupDir + "/" + latest;
}

bool restoreLatestBackup(const std::string& filePath, const std::string& backupDir) {
    std::string latestBackup = findLatestBackup(filePath, backupDir);
    if (latestBackup.empty()) return false;

    return restoreFile(latestBackup, filePath);
}

bool rollbackToVersion(const std::string& filePath, const std::string& backupDir, int version) {
    size_t slashPos = filePath.find_last_of("/\\");
    std::string baseName = (slashPos != std::string::npos) ? filePath.substr(slashPos + 1) : filePath;

    std::vector<std::string> backups;

#ifdef _WIN32
    std::string pattern = backupDir + "\\" + baseName + ".*.bak";
    WIN32_FIND_DATA ffd;
    HANDLE hFind = FindFirstFile(pattern.c_str(), &ffd);
    if (hFind != INVALID_HANDLE_VALUE) {
        do {
            backups.push_back(ffd.cFileName);
        } while (FindNextFile(hFind, &ffd) != 0);
        FindClose(hFind);
    }
#else
    DIR* dp = opendir(backupDir.c_str());
    if (dp) {
        struct dirent* entry;
        std::string prefix = baseName + ".";
        while ((entry = readdir(dp)) != nullptr) {
            std::string name = entry->d_name;
            if (name.find(prefix) == 0 && name.size() > prefix.size() + 4 &&
                name.substr(name.size() - 4) == ".bak") {
                backups.push_back(name);
            }
        }
        closedir(dp);
    }
#endif

    std::sort(backups.begin(), backups.end());
    if (version < 0 || version >= static_cast<int>(backups.size())) return false;

    std::string backupPath = backupDir + "/" + backups[version];
    return restoreFile(backupPath, filePath);
}

}
}
