#pragma once
#include <string>
#include <vector>
#include <functional>

namespace ravex {
namespace fileprot {

enum class FileStatus {
    Ok,
    Modified,
    Missing,
    Added,
    Corrupted
};

struct FileEntry {
    std::string path;
    std::string hash;
    int64_t size;
    int64_t modifiedTime;
    FileStatus status;
};

struct ScanResult {
    std::vector<FileEntry> changed;
    std::vector<FileEntry> missing;
    std::vector<FileEntry> added;
    int totalFiles;
    int64_t scanTimeMs;
};

using ProgressCallback = std::function<void(int percent, const std::string& file)>;

bool initialize(const std::string& dbPath);
void shutdown();
bool scanDirectory(const std::string& dir, std::vector<FileEntry>& entries, ProgressCallback cb = nullptr);
bool verifyIntegrity(const std::vector<FileEntry>& baseline, ScanResult& result, ProgressCallback cb = nullptr);
bool saveDatabase(const std::string& path, const std::vector<FileEntry>& entries);
std::vector<FileEntry> loadDatabase(const std::string& path);
bool backupFile(const std::string& filePath, const std::string& backupDir);
bool restoreFile(const std::string& backupPath, const std::string& targetPath);

}
}
