#include "include/fileprot.hpp"
#include "include/fileprot_hasher.hpp"
#include "include/fileprot_integrity.hpp"
#include "include/fileprot_config.hpp"
#include <fstream>
#include <sstream>
#include <cstring>
#include <sys/stat.h>

#ifdef _WIN32
#include <windows.h>
#include <direct.h>
#define mkdir _mkdir
#else
#include <unistd.h>
#include <dirent.h>
#endif

namespace ravex {
namespace fileprot {

static FileHasher hasher;
static bool initialized = false;

bool initialize(const std::string& dbPath) {
    if (initialized) return true;
    hasher = FileHasher();
    initialized = true;

#ifdef _WIN32
    mkdir("fileprot_backups");
#else
    mkdir("fileprot_backups", 0755);
#endif

    return true;
}

void shutdown() {
    initialized = false;
}

bool scanDirectory(const std::string& dir, std::vector<FileEntry>& entries, ProgressCallback cb) {
#ifdef _WIN32
    std::string pattern = dir + "\\*.*";
    WIN32_FIND_DATA ffd;
    HANDLE hFind = FindFirstFile(pattern.c_str(), &ffd);
    if (hFind == INVALID_HANDLE_VALUE) return false;

    int count = 0;
    do {
        if (strcmp(ffd.cFileName, ".") == 0 || strcmp(ffd.cFileName, "..") == 0) continue;

        std::string fullPath = dir + "\\" + ffd.cFileName;
        if (ffd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
            scanDirectory(fullPath, entries, cb);
        } else {
            FileEntry entry;
            entry.path = fullPath;
            entry.hash = hasher.hashFile(fullPath);
            entry.size = (static_cast<int64_t>(ffd.nFileSizeHigh) << 32) | ffd.nFileSizeLow;
            entry.status = FileStatus::Ok;
            entries.push_back(entry);
        }
        if (cb) cb((count++ * 100) / 1000, fullPath);
    } while (FindNextFile(hFind, &ffd) != 0);
    FindClose(hFind);
#else
    DIR* dp = opendir(dir.c_str());
    if (!dp) return false;

    struct dirent* entry;
    int count = 0;
    while ((entry = readdir(dp)) != nullptr) {
        if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) continue;

        std::string fullPath = dir + "/" + entry->d_name;

        struct stat st;
        if (stat(fullPath.c_str(), &st) != 0) continue;

        if (S_ISDIR(st.st_mode)) {
            scanDirectory(fullPath, entries, cb);
        } else if (S_ISREG(st.st_mode)) {
            FileEntry fe;
            fe.path = fullPath;
            fe.hash = hasher.hashFile(fullPath);
            fe.size = st.st_size;
            fe.modifiedTime = st.st_mtime;
            fe.status = FileStatus::Ok;
            entries.push_back(fe);
        }
        if (cb) cb((count++ * 100) / 1000, fullPath);
    }
    closedir(dp);
#endif

    return true;
}

bool verifyIntegrity(const std::vector<FileEntry>& baseline, ScanResult& result, ProgressCallback cb) {
    result.totalFiles = 0;
    result.scanTimeMs = 0;

    for (const auto& entry : baseline) {
        result.totalFiles++;

#ifdef _WIN32
        struct _stat st;
        bool exists = (_stat(entry.path.c_str(), &st) == 0);
#else
        struct stat st;
        bool exists = (stat(entry.path.c_str(), &st) == 0);
#endif

        if (!exists) {
            FileEntry missing = entry;
            missing.status = FileStatus::Missing;
            result.missing.push_back(missing);
            continue;
        }

        std::string currentHash = hasher.hashFile(entry.path);
        if (currentHash != entry.hash) {
            FileEntry changed = entry;
            changed.hash = currentHash;
            changed.size = st.st_size;
            changed.status = FileStatus::Modified;
            result.changed.push_back(changed);
        }
    }

    return true;
}

bool saveDatabase(const std::string& path, const std::vector<FileEntry>& entries) {
    std::ofstream file(path);
    if (!file.is_open()) return false;

    file << entries.size() << "\n";
    for (const auto& e : entries) {
        file << e.path << "\n";
        file << e.hash << "\n";
        file << e.size << "\n";
        file << e.modifiedTime << "\n";
    }
    return file.good();
}

std::vector<FileEntry> loadDatabase(const std::string& path) {
    std::vector<FileEntry> entries;
    std::ifstream file(path);
    if (!file.is_open()) return entries;

    int count;
    file >> count;
    file.ignore();

    for (int i = 0; i < count; i++) {
        FileEntry e;
        std::getline(file, e.path);
        std::getline(file, e.hash);
        file >> e.size;
        file >> e.modifiedTime;
        file.ignore();
        e.status = FileStatus::Ok;
        entries.push_back(e);
    }
    return entries;
}

bool backupFile(const std::string& filePath, const std::string& backupDir) {
#ifdef _WIN32
    mkdir(backupDir.c_str());
#else
    mkdir(backupDir.c_str(), 0755);
#endif

    std::ifstream src(filePath, std::ios::binary);
    if (!src.is_open()) return false;

    size_t slashPos = filePath.find_last_of("/\\");
    std::string fileName = (slashPos != std::string::npos) ? filePath.substr(slashPos + 1) : filePath;
    std::string backupPath = backupDir + "/" + fileName;

    std::ofstream dst(backupPath, std::ios::binary);
    if (!dst.is_open()) return false;

    dst << src.rdbuf();
    return dst.good();
}

bool restoreFile(const std::string& backupPath, const std::string& targetPath) {
    std::ifstream src(backupPath, std::ios::binary);
    if (!src.is_open()) return false;

    std::ofstream dst(targetPath, std::ios::binary);
    if (!dst.is_open()) return false;

    dst << src.rdbuf();
    return dst.good();
}

}
}
