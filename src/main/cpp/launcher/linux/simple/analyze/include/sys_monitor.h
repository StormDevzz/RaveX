#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace analyze {

struct SysSnapshot {
    double cpu_percent;
    double mem_used_mb;
    double mem_total_mb;
    double disk_read_mb;
    double disk_write_mb;
    long uptime_seconds;
};

SysSnapshot take_snapshot();
std::string snapshot_to_string(const SysSnapshot &s);
void log_system_state(const std::string &label);

} // namespace analyze
} // namespace simple
} // namespace launcher
} // namespace ravex
