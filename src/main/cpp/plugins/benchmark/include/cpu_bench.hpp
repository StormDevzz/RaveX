#pragma once
#include "benchmark.hpp"

namespace ravex {
namespace benchmark {

class CpuBench {
public:
    CpuBench();
    double runIntegerOps(int iterations);
    double runFloatOps(int iterations);
    double runPrimeSieve(int limit);
    double runSha256(int iterations);
    BenchResult runAll();

private:
    double measureTime(std::function<void()> fn);
    bool isPrime(int n);
    std::string sha256Hex(const std::string& input);
};

}
}
