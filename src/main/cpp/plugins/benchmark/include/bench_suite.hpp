#pragma once
#include "benchmark.hpp"
#include "cpu_bench.hpp"
#include "memory_bench.hpp"
#include "disk_bench.hpp"
#include "bench_config.hpp"
#include "bench_result.hpp"
#include <vector>

namespace ravex {
namespace benchmark {

class BenchSuite {
public:
    explicit BenchSuite(const BenchConfig& config);
    std::vector<BenchResult> runAll(ProgressCallback cb);
    std::vector<BenchResult> runCpu(ProgressCallback cb);
    std::vector<BenchResult> runMemory(ProgressCallback cb);
    std::vector<BenchResult> runDisk(ProgressCallback cb);
    BenchResult computeAggregate(const std::vector<BenchResult>& results);
    std::string getHardwareInfo();

private:
    BenchConfig config;
    CpuBench cpu;
    MemoryBench mem;
    DiskBench disk;
};

}
}
