#include "include/fileprot_integrity.hpp"
#include <algorithm>
#include <set>
#include <map>

#ifdef _WIN32
#include <windows.h>
#else
#include <unistd.h>
#include <sys/stat.h>
#endif

namespace ravex {
namespace fileprot {

IntegrityChecker::IntegrityChecker() {}

bool IntegrityChecker::buildBaseline(const std::string& dir, std::vector<FileEntry>& baseline, ProgressCallback cb) {
    return scanDirectory(dir, baseline, cb);
}

bool IntegrityChecker::checkIntegrity(const std::vector<FileEntry>& baseline, ScanResult& result, ProgressCallback cb) {
    return verifyIntegrity(baseline, result, cb);
}

bool IntegrityChecker::repairFiles(const std::vector<FileEntry>& damaged, const std::string& sourceDir) {
    for (const auto& entry : damaged) {
        std::string sourcePath = sourceDir + "/" + entry.path;
        if (!restoreFile(sourcePath, entry.path)) {
            return false;
        }
    }
    return true;
}

std::vector<FileEntry> IntegrityChecker::findDuplicates(const std::vector<FileEntry>& entries) {
    std::map<std::string, std::vector<FileEntry>> hashMap;
    for (const auto& e : entries) {
        hashMap[e.hash].push_back(e);
    }

    std::vector<FileEntry> duplicates;
    for (const auto& pair : hashMap) {
        if (pair.second.size() > 1) {
            for (size_t i = 1; i < pair.second.size(); i++) {
                duplicates.push_back(pair.second[i]);
            }
        }
    }
    return duplicates;
}

std::map<std::string, int> IntegrityChecker::getExtensionStats(const std::vector<FileEntry>& entries) {
    std::map<std::string, int> stats;
    for (const auto& e : entries) {
        size_t dot = e.path.find_last_of('.');
        std::string ext = (dot != std::string::npos) ? e.path.substr(dot) : "(none)";
        stats[ext]++;
    }
    return stats;
}

std::string IntegrityChecker::normalizePath(const std::string& path) {
    std::string result = path;
#ifdef _WIN32
    std::replace(result.begin(), result.end(), '/', '\\');
#else
    std::replace(result.begin(), result.end(), '\\', '/');
#endif
    return result;
}

}
}
