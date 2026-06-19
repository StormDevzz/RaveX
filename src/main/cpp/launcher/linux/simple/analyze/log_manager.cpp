#include "include/log_manager.h"
#include <fstream>
#include <sstream>
#include <ctime>
#include <unistd.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace analyze {

static std::string log_dir;
static const size_t MAX_LOG_SIZE = 1024 * 1024;

void init_logs(const std::string &kickx_dir) {
    log_dir = kickx_dir + "/logs";
    std::string cmd = "mkdir -p \"" + log_dir + "\"";
    system(cmd.c_str());
}

static std::string log_path(const std::string &section) {
    return log_dir + "/" + section + ".log";
}

void write_log(const std::string &section, const std::string &message) {
    if (log_dir.empty()) return;

    std::string path = log_path(section);
    std::ofstream f(path, std::ios::app);
    if (!f.is_open()) return;

    time_t now = time(nullptr);
    struct tm *tm = localtime(&now);
    char buf[32];
    strftime(buf, sizeof(buf), "%H:%M:%S", tm);

    f << "[" << buf << "] " << message << std::endl;
    f.close();

    rotate_logs(section, MAX_LOG_SIZE);
}

void write_log_nothrow(const std::string &section, const std::string &message) {
    try {
        write_log(section, message);
    } catch (...) {}
}

std::string read_log(const std::string &section) {
    std::string path = log_path(section);
    std::ifstream f(path);
    if (!f.is_open()) return "no log";
    std::string content((std::istreambuf_iterator<char>(f)),
                         std::istreambuf_iterator<char>());
    return content;
}

void rotate_logs(const std::string &section, size_t max_size) {
    std::string path = log_path(section);
    long size = 0;
    std::ifstream f(path, std::ios::ate | std::ios::binary);
    if (f.is_open()) {
        size = f.tellg();
        f.close();
    }
    if (size > static_cast<long>(max_size)) {
        std::string old_path = path + ".old";
        std::string cmd = "mv \"" + path + "\" \"" + old_path + "\"";
        system(cmd.c_str());
    }
}

} // namespace analyze
} // namespace simple
} // namespace launcher
} // namespace ravex
