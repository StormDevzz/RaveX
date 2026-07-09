#include "include/fileprot.hpp"
#include "include/fileprot_hasher.hpp"
#include <map>
#include <fstream>
#include <sstream>

namespace ravex {
namespace fileprot {

class ChecksumDatabase {
public:
    ChecksumDatabase() {}

    bool load(const std::string& path) {
        db.clear();
        std::ifstream file(path);
        if (!file.is_open()) return false;

        std::string line;
        while (std::getline(file, line)) {
            if (line.length() < 65) continue;
            std::string hash = line.substr(0, 64);
            std::string filePath = line.substr(65);
            db[filePath] = hash;
        }
        return true;
    }

    bool save(const std::string& path) const {
        std::ofstream file(path);
        if (!file.is_open()) return false;

        for (const auto& entry : db) {
            file << entry.second << "  " << entry.first << "\n";
        }
        return file.good();
    }

    void addEntry(const std::string& filePath, const std::string& hash) {
        db[filePath] = hash;
    }

    void removeEntry(const std::string& filePath) {
        db.erase(filePath);
    }

    std::string getHash(const std::string& filePath) const {
        auto it = db.find(filePath);
        return (it != db.end()) ? it->second : "";
    }

    bool verifyEntry(const std::string& filePath) const {
        auto it = db.find(filePath);
        if (it == db.end()) return false;

        FileHasher hasher;
        std::string currentHash = hasher.hashFile(filePath);
        return currentHash == it->second;
    }

    bool hasEntry(const std::string& filePath) const {
        return db.count(filePath) > 0;
    }

    size_t size() const {
        return db.size();
    }

    void clear() {
        db.clear();
    }

    int verifyAll(std::vector<std::string>& failed) const {
        int checked = 0;
        failed.clear();
        FileHasher hasher;

        for (const auto& entry : db) {
            std::string currentHash = hasher.hashFile(entry.first);
            if (currentHash.empty()) {
                failed.push_back(entry.first + " (missing)");
            } else if (currentHash != entry.second) {
                failed.push_back(entry.first + " (mismatch)");
            }
            checked++;
        }
        return checked;
    }

private:
    std::map<std::string, std::string> db;
};

}
}
