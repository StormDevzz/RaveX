#pragma once
#include <string>
#include <chrono>

namespace ravex {
namespace launcher {
namespace simple {
namespace analyze {

class PerfTracker {
public:
    PerfTracker(const std::string &name);
    ~PerfTracker();
    void checkpoint(const std::string &label);
    double elapsed_ms() const;
    std::string report() const;
private:
    std::string name_;
    std::chrono::steady_clock::time_point start_;
    bool stopped_ = false;
};

void log_perf(const std::string &section, const std::string &detail, long ms);

} // namespace analyze
} // namespace simple
} // namespace launcher
} // namespace ravex
