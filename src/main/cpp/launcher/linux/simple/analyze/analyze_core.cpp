#include "include/crash_logger.hpp"
#include "include/sys_monitor.hpp"
#include "include/perf_tracker.hpp"
#include "include/log_manager.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace analyze {

void init_analyze(const std::string &kickx_dir) {
    init_logs(kickx_dir);
    setup_crash_handler();
    log_system_state("launch");
}

void shutdown_analyze() {
    log_system_state("shutdown");
}

}
}
}
}
