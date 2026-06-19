#include "include/perf_tracker.h"
#include "include/log_manager.h"
#include <sstream>

namespace ravex {
namespace launcher {
namespace simple {
namespace analyze {

PerfTracker::PerfTracker(const std::string &name)
    : name_(name), start_(std::chrono::steady_clock::now()) {}

PerfTracker::~PerfTracker() {
    if (!stopped_) {
        stopped_ = true;
        write_log("PERF", name_ + " took " + std::to_string(elapsed_ms()) + " ms");
    }
}

void PerfTracker::checkpoint(const std::string &label) {
    auto now = std::chrono::steady_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(now - start_).count();
    write_log("PERF", name_ + " @ " + label + ": " + std::to_string(ms) + " ms");
}

double PerfTracker::elapsed_ms() const {
    auto now = std::chrono::steady_clock::now();
    return std::chrono::duration<double, std::milli>(now - start_).count();
}

std::string PerfTracker::report() const {
    std::ostringstream os;
    os << name_ << ": " << elapsed_ms() << " ms";
    return os.str();
}

void log_perf(const std::string &section, const std::string &detail, long ms) {
    write_log("PERF", section + "/" + detail + ": " + std::to_string(ms) + " ms");
}

} // namespace analyze
} // namespace simple
} // namespace launcher
} // namespace ravex
