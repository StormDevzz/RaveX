#include "include/benchmark.hpp"
#include "include/bench_common.hpp"
#include <iostream>
#include <fstream>
#include <ctime>

namespace ravex {
namespace benchmark {

enum class LogLevel {
    Info,
    Warn,
    Error,
    Debug
};

class BenchLogger {
public:
    BenchLogger() : logToFile(false) {}

    void setLogFile(const std::string& path) {
        logPath = path;
        logToFile = true;
    }

    void info(const std::string& msg) { log(LogLevel::Info, msg); }
    void warn(const std::string& msg) { log(LogLevel::Warn, msg); }
    void error(const std::string& msg) { log(LogLevel::Error, msg); }
    void debug(const std::string& msg) { log(LogLevel::Debug, msg); }

private:
    bool logToFile;
    std::string logPath;

    std::string levelStr(LogLevel lvl) {
        switch (lvl) {
            case LogLevel::Info: return "INFO";
            case LogLevel::Warn: return "WARN";
            case LogLevel::Error: return "ERROR";
            case LogLevel::Debug: return "DEBUG";
        }
        return "UNKNOWN";
    }

    void log(LogLevel lvl, const std::string& msg) {
        std::time_t t = std::time(nullptr);
        char buf[20];
        std::strftime(buf, sizeof(buf), "%H:%M:%S", std::localtime(&t));
        std::string line = std::string("[") + buf + "][" + levelStr(lvl) + "] " + msg;

        std::cout << line << std::endl;

        if (logToFile && !logPath.empty()) {
            std::ofstream file(logPath, std::ios::app);
            if (file.is_open()) {
                file << line << std::endl;
            }
        }
    }
};

}
}
