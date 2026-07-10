#pragma once
#include <string>
#include <functional>

namespace ravex {
namespace launcher {
namespace simple {
namespace analyze {

void setup_crash_handler();
void log_crash(const std::string &message);
void set_log_enabled(bool enabled);
bool is_log_enabled();
void init_analyze(const std::string &kickx_dir);
void shutdown_analyze();

}
}
}
}
