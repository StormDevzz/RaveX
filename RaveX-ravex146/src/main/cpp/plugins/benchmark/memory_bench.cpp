#include "include/memory_bench.hpp"
#include "include/bench_common.hpp"
#include <cstring>
#include <cstdlib>

namespace ravex {
namespace benchmark {

MemoryBench::MemoryBench() {}

double MemoryBench::measureTime(std::function<void()> fn) {
    Timer timer;
    fn();
    return timer.elapsedMs();
}

double MemoryBench::runSequentialRead(size_t sizeMb) {
    buffer.resize(sizeMb * 1024 * 1024);
    for (size_t i = 0; i < buffer.size(); i++) {
        buffer[i] = static_cast<char>(i & 0xFF);
    }
    volatile char sink = 0;
    auto work = [&]() {
        for (size_t i = 0; i < buffer.size(); i++) {
            sink += buffer[i];
        }
    };
    double ms = measureTime(work);
    (void)sink;
    buffer.clear();
    buffer.shrink_to_fit();
    return ms;
}

double MemoryBench::runSequentialWrite(size_t sizeMb) {
    buffer.resize(sizeMb * 1024 * 1024);
    auto work = [&]() {
        for (size_t i = 0; i < buffer.size(); i++) {
            buffer[i] = static_cast<char>(i & 0xFF);
        }
    };
    double ms = measureTime(work);
    buffer.clear();
    buffer.shrink_to_fit();
    return ms;
}

double MemoryBench::runRandomAccess(size_t sizeMb) {
    buffer.resize(sizeMb * 1024 * 1024);
    std::memset(buffer.data(), 0, buffer.size());
    size_t stride = 64;
    size_t count = buffer.size() / stride;
    volatile char sink = 0;
    auto work = [&]() {
        for (size_t i = 0; i < count; i++) {
            size_t idx = (i * 2654435761ULL) % (buffer.size() - stride);
            sink += buffer[idx];
        }
    };
    double ms = measureTime(work);
    (void)sink;
    buffer.clear();
    buffer.shrink_to_fit();
    return ms;
}

double MemoryBench::runLatency(size_t sizeMb) {
    buffer.resize(sizeMb * 1024 * 1024);
    std::memset(buffer.data(), 0, buffer.size());
    size_t count = 1000000;
    size_t idx = 0;
    size_t mask = buffer.size() - 1;
    Timer timer;
    for (size_t i = 0; i < count; i++) {
        idx = (idx * 2654435761ULL + i) & mask;
        buffer[idx] = static_cast<char>(buffer[idx] + 1);
    }
    double ms = timer.elapsedMs();
    buffer.clear();
    buffer.shrink_to_fit();
    return ms;
}

BenchResult MemoryBench::runAll() {
    BenchResult r;
    r.type = BenchType::Memory;
    r.name = "Memory";
    r.timeMs = runSequentialRead(128) + runSequentialWrite(128) +
               runRandomAccess(64) + runLatency(32);
    r.score = 1024.0 * 128.0 / (r.timeMs + 0.001);
    r.unit = "MB/s";
    return r;
}

}
}
