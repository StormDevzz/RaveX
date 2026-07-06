#include "include/fileprot.hpp"
#include "include/fileprot_hasher.hpp"
#include <fstream>
#include <chrono>

namespace ravex {
namespace fileprot {

bool quickVerify(const std::string& filePath, const std::string& expectedHash) {
    FileHasher hasher;
    std::string actualHash = hasher.hashFile(filePath);
    return actualHash == expectedHash;
}

bool generateChecksumFile(const std::string& dir, const std::string& outputPath) {
    std::vector<FileEntry> entries;
    if (!scanDirectory(dir, entries)) return false;

    std::ofstream out(outputPath);
    if (!out.is_open()) return false;

    for (const auto& e : entries) {
        size_t relPos = e.path.find(dir);
        std::string relPath = (relPos == 0) ? e.path.substr(dir.length() + 1) : e.path;
        out << e.hash << "  " << relPath << "\n";
    }
    return out.good();
}

int verifyChecksumFile(const std::string& checksumPath, const std::string& baseDir) {
    std::ifstream in(checksumPath);
    if (!in.is_open()) return -1;

    FileHasher hasher;
    int failures = 0;
    std::string line;

    while (std::getline(in, line)) {
        if (line.length() < 66) continue;

        std::string expectedHash = line.substr(0, 64);
        std::string filePath;

        size_t sepPos = line.find("  ");
        if (sepPos != std::string::npos) {
            filePath = line.substr(sepPos + 2);
        }

        if (filePath.empty()) continue;
        std::string fullPath = baseDir + "/" + filePath;

        std::string actualHash = hasher.hashFile(fullPath);
        if (actualHash.empty()) {
            failures++;
        } else if (actualHash != expectedHash) {
            failures++;
        }
    }

    return failures;
}

}
}
