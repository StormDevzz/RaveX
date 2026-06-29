#pragma once
#include "fileprot.hpp"
#include "fileprot_hasher.hpp"
#include <map>

namespace ravex {
namespace fileprot {

class IntegrityChecker {
public:
    IntegrityChecker();
    bool buildBaseline(const std::string& dir, std::vector<FileEntry>& baseline, ProgressCallback cb);
    bool checkIntegrity(const std::vector<FileEntry>& baseline, ScanResult& result, ProgressCallback cb);
    bool repairFiles(const std::vector<FileEntry>& damaged, const std::string& sourceDir);
    std::vector<FileEntry> findDuplicates(const std::vector<FileEntry>& entries);
    std::map<std::string, int> getExtensionStats(const std::vector<FileEntry>& entries);

private:
    FileHasher hasher;
    std::string normalizePath(const std::string& path);
};

}
}
