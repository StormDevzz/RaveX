#include "include/fileprot.hpp"
#include <fstream>
#include <ctime>

namespace ravex {
namespace fileprot {

class FileProtLogger {
public:
    FileProtLogger() : enabled(false) {}

    void setLogFile(const std::string& path) {
        logPath = path;
        enabled = true;
    }

    void log(const std::string& event, const std::string& filePath) {
        if (!enabled) return;

        std::time_t t = std::time(nullptr);
        char buf[64];
        std::strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", std::localtime(&t));

        std::ofstream file(logPath, std::ios::app);
        if (file.is_open()) {
            file << "[" << buf << "] " << event << ": " << filePath << "\n";
        }
    }

    void logIntegrity(const std::string& filePath, bool passed) {
        log(passed ? "INTEGRITY_OK" : "INTEGRITY_FAIL", filePath);
    }

    void logBackup(const std::string& filePath, const std::string& backupPath) {
        std::time_t t = std::time(nullptr);
        char buf[64];
        std::strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", std::localtime(&t));

        std::ofstream file(logPath, std::ios::app);
        if (file.is_open()) {
            file << "[" << buf << "] BACKUP: " << filePath << " -> " << backupPath << "\n";
        }
    }

    void logRestore(const std::string& backupPath, const std::string& targetPath) {
        std::time_t t = std::time(nullptr);
        char buf[64];
        std::strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", std::localtime(&t));

        std::ofstream file(logPath, std::ios::app);
        if (file.is_open()) {
            file << "[" << buf << "] RESTORE: " << backupPath << " -> " << targetPath << "\n";
        }
    }

private:
    bool enabled;
    std::string logPath;
};

}
}
