#pragma once
#include <string>
#include <vector>
#include <functional>

namespace ravex {
namespace benchmark {

enum class BenchType {
    CPU,
    Memory,
    Disk,
    Network,
    All
};

enum class CpuTest {
    IntegerOps,
    FloatOps,
    PrimeSieve,
    Sha256Hash
};

enum class MemTest {
    SequentialRead,
    SequentialWrite,
    RandomAccess,
    Latency
};

enum class DiskTest {
    SequentialRead,
    SequentialWrite,
    RandomRead4K,
    RandomWrite4K
};

struct BenchResult {
    BenchType type;
    std::string name;
    double score;
    double timeMs;
    std::string unit;
};

using ProgressCallback = std::function<void(int percent, const std::string& stage)>;

bool runBenchmark(BenchType type, std::vector<BenchResult>& results, ProgressCallback cb = nullptr);
std::string formatResults(const std::vector<BenchResult>& results);

}
}
