#pragma once
#include "benchmark.hpp"
#include <string>

namespace ravex {
namespace benchmark {

class NetworkBench {
public:
    NetworkBench();
    double pingHost(const std::string& host, int count);
    double measureLatency(const std::string& host, int count);
    BenchResult runAll(const std::string& host);
private:
    double measureTime(std::function<void()> fn);
};

}
}
