#include "include/fileprot.hpp"
#include "include/fileprot_hasher.hpp"
#include <fstream>
#include <set>

namespace ravex {
namespace fileprot {

int scanForChanges(const std::string& baselineDb, const std::string& targetDir) {
    std::vector<FileEntry> baseline = loadDatabase(baselineDb);
    if (baseline.empty()) return -1;

    std::vector<FileEntry> current;
    if (!scanDirectory(targetDir, current)) return -1;

    ScanResult result;
    verifyIntegrity(baseline, result, nullptr);

    return static_cast<int>(result.changed.size() + result.missing.size());
}

bool incrementalScan(const std::string& dir, const std::string& prevDb,
                     std::vector<FileEntry>& newEntries, std::vector<FileEntry>& removed) {
    std::vector<FileEntry> previous = loadDatabase(prevDb);
    std::vector<FileEntry> current;
    if (!scanDirectory(dir, current)) return false;

    std::set<std::string> prevPaths;
    for (const auto& e : previous) prevPaths.insert(e.path);

    std::set<std::string> currPaths;
    for (const auto& e : current) currPaths.insert(e.path);

    for (const auto& e : current) {
        if (prevPaths.find(e.path) == prevPaths.end()) {
            newEntries.push_back(e);
        }
    }

    for (const auto& e : previous) {
        if (currPaths.find(e.path) == currPaths.end()) {
            removed.push_back(e);
        }
    }

    return true;
}

}
}
