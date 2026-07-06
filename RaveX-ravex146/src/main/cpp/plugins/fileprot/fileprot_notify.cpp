#include "include/fileprot.hpp"
#include <iostream>
#include <ctime>

namespace ravex {
namespace fileprot {

class Notification {
public:
    enum Level { Info, Warning, Error, Critical };

    std::string message;
    std::string filePath;
    Level level;
    std::string timestamp;
};

class Notifier {
public:
    Notifier() : enabled(true) {}

    void setEnabled(bool e) { enabled = e; }

    void send(const Notification& notif) {
        if (!enabled) return;

        std::time_t t = std::time(nullptr);
        char buf[20];
        std::strftime(buf, sizeof(buf), "%H:%M:%S", std::localtime(&t));

        std::string levelStr;
        switch (notif.level) {
            case Notification::Info: levelStr = "INFO"; break;
            case Notification::Warning: levelStr = "WARN"; break;
            case Notification::Error: levelStr = "ERROR"; break;
            case Notification::Critical: levelStr = "CRIT"; break;
        }

        std::cout << "[" << buf << "][" << levelStr << "] "
                  << notif.message;
        if (!notif.filePath.empty()) {
            std::cout << " (" << notif.filePath << ")";
        }
        std::cout << std::endl;
    }

    void fileChanged(const std::string& path) {
        Notification n;
        n.message = "File changed";
        n.filePath = path;
        n.level = Notification::Info;
        send(n);
    }

    void fileDeleted(const std::string& path) {
        Notification n;
        n.message = "File deleted";
        n.filePath = path;
        n.level = Notification::Warning;
        send(n);
    }

    void integrityFailure(const std::string& path) {
        Notification n;
        n.message = "Integrity check failed";
        n.filePath = path;
        n.level = Notification::Error;
        send(n);
    }

    void backupCreated(const std::string& path) {
        Notification n;
        n.message = "Backup created";
        n.filePath = path;
        n.level = Notification::Info;
        send(n);
    }

private:
    bool enabled;
};

}
}
