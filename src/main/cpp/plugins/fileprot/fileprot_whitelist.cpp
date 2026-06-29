#include "include/fileprot.hpp"
#include <set>
#include <fstream>

namespace ravex {
namespace fileprot {

class WhitelistManager {
public:
    WhitelistManager() {}

    bool load(const std::string& path) {
        whitelist.clear();
        std::ifstream file(path);
        if (!file.is_open()) return false;

        std::string line;
        while (std::getline(file, line)) {
            if (!line.empty()) whitelist.insert(line);
        }
        return true;
    }

    bool save(const std::string& path) const {
        std::ofstream file(path);
        if (!file.is_open()) return false;

        for (const auto& entry : whitelist) {
            file << entry << "\n";
        }
        return file.good();
    }

    void add(const std::string& path) {
        whitelist.insert(path);
    }

    void remove(const std::string& path) {
        whitelist.erase(path);
    }

    bool contains(const std::string& path) const {
        return whitelist.count(path) > 0;
    }

    size_t size() const {
        return whitelist.size();
    }

    void clear() {
        whitelist.clear();
    }

    bool isWhitelisted(const FileEntry& entry) const {
        for (const auto& w : whitelist) {
            if (entry.path.find(w) != std::string::npos) return true;
        }
        return false;
    }

private:
    std::set<std::string> whitelist;
};

}
}
