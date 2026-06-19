#include "include/sys_monitor.h"
#include "include/log_manager.h"
#include <fstream>
#include <sstream>
#include <unistd.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace analyze {

static long read_long_from_file(const std::string &path) {
    std::ifstream f(path);
    long val = 0;
    if (f.is_open()) { f >> val; }
    return val;
}

SysSnapshot take_snapshot() {
    SysSnapshot s{};
    long mem_total_kb = 0, mem_avail_kb = 0;

    std::ifstream meminfo("/proc/meminfo");
    std::string line;
    while (std::getline(meminfo, line)) {
        if (line.compare(0, 10, "MemTotal: ") == 0)
            mem_total_kb = std::stol(line.substr(10));
        else if (line.compare(0, 13, "MemAvailable: ") == 0)
            mem_avail_kb = std::stol(line.substr(13));
    }

    s.mem_total_mb = mem_total_kb / 1024.0;
    s.mem_used_mb = (mem_total_kb - mem_avail_kb) / 1024.0;

    long cpu_user = read_long_from_file("/proc/stat");
    (void)cpu_user;
    s.cpu_percent = 0.0;

    s.uptime_seconds = read_long_from_file("/proc/uptime");

    s.disk_read_mb = 0.0;
    s.disk_write_mb = 0.0;

    return s;
}

std::string snapshot_to_string(const SysSnapshot &s) {
    std::ostringstream os;
    os << "mem: " << s.mem_used_mb << "/" << s.mem_total_mb << " MB";
    os << " | cpu: " << s.cpu_percent << "%";
    os << " | uptime: " << (s.uptime_seconds / 3600) << "h";
    return os.str();
}

void log_system_state(const std::string &label) {
    auto snap = take_snapshot();
    write_log("SYS", label + " -> " + snapshot_to_string(snap));
}

} // namespace analyze
} // namespace simple
} // namespace launcher
} // namespace ravex
