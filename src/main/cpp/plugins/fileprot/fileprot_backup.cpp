#include "include/fileprot.hpp"
#include <fstream>
#include <ctime>
#include <algorithm>
#include <sys/stat.h>

#ifdef _WIN32
#include <direct.h>
#endif

#ifdef _WIN32
#include <windows.h>
#else
#include <dirent.h>
#endif

namespace ravex {
namespace fileprot {

bool createBackup(const std::string& sourcePath, const std::string& backupDir) {
    return backupFile(sourcePath, backupDir);
}

int createBackupWithVersion(const std::string& sourcePath, const std::string& backupDir, int maxVersions) {
#ifdef _WIN32
    _mkdir(backupDir.c_str());
#else
    mkdir(backupDir.c_str(), 0755);
#endif

    size_t slashPos = sourcePath.find_last_of("/\\");
    std::string baseName = (slashPos != std::string::npos) ? sourcePath.substr(slashPos + 1) : sourcePath;

    std::time_t t = std::time(nullptr);
    char timeBuf[32];
    std::strftime(timeBuf, sizeof(timeBuf), "%Y%m%d_%H%M%S", std::localtime(&t));

    std::string backupPath = backupDir + "/" + baseName + "." + timeBuf + ".bak";
    if (!backupFile(sourcePath, backupPath)) return -1;

#ifdef _WIN32
    std::string pattern = backupDir + "\\" + baseName + ".*.bak";
    WIN32_FIND_DATA ffd;
    HANDLE hFind = FindFirstFile(pattern.c_str(), &ffd);
    std::vector<std::string> backups;
    if (hFind != INVALID_HANDLE_VALUE) {
        do {
            backups.push_back(ffd.cFileName);
        } while (FindNextFile(hFind, &ffd) != 0);
        FindClose(hFind);
    }
#else
    std::vector<std::string> backups;
    DIR* dp = opendir(backupDir.c_str());
    if (dp) {
        struct dirent* entry;
        std::string prefix = baseName + ".";
        while ((entry = readdir(dp)) != nullptr) {
            std::string name = entry->d_name;
            if (name.find(prefix) == 0 && name.size() > prefix.size()) {
                backups.push_back(name);
            }
        }
        closedir(dp);
    }
#endif

    std::sort(backups.begin(), backups.end());

    while (static_cast<int>(backups.size()) > maxVersions) {
        std::string oldBackup = backupDir + "/" + backups.front();
        std::remove(oldBackup.c_str());
        backups.erase(backups.begin());
    }

    return static_cast<int>(backups.size());
}

}
}
