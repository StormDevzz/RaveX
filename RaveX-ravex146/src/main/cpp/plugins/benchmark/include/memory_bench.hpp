#pragma once
#include "benchmark.hpp"
#include <vector>

namespace ravex {
namespace benchmark {

class MemoryBench {
public:
    MemoryBench();
    double runSequentialRead(size_t sizeMb);
    double runSequentialWrite(size_t sizeMb);
    double runRandomAccess(size_t sizeMb);
    double runLatency(size_t sizeMb);
    BenchResult runAll();

private:
    double measureTime(std::function<void()> fn);
    std::vector<char> buffer;
};

}
}
