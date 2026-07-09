#include "include/benchmark.hpp"
#include "include/bench_suite.hpp"
#include "include/bench_config.hpp"
#include "include/bench_report.hpp"

namespace ravex {
namespace benchmark {

bool runBenchmark(BenchType type, std::vector<BenchResult>& results, ProgressCallback cb) {
    BenchConfig config = defaultConfig();
    BenchSuite suite(config);
    switch (type) {
        case BenchType::CPU:
            results = suite.runCpu(cb);
            break;
        case BenchType::Memory:
            results = suite.runMemory(cb);
            break;
        case BenchType::Disk:
            results = suite.runDisk(cb);
            break;
        case BenchType::All:
            results = suite.runAll(cb);
            break;
        default:
            return false;
    }
    return !results.empty();
}

std::string formatResults(const std::vector<BenchResult>& results) {
    return BenchResultFormatter::formatAll(results);
}

}
}
