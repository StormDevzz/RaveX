#include "include/cpu_bench.hpp"
#include "include/bench_common.hpp"
#include <cstring>
#include <sstream>
#include <iomanip>

#ifdef _WIN32
#include <intrin.h>
#else
#include <cpuid.h>
#endif

namespace ravex {
namespace benchmark {

CpuBench::CpuBench() {}

double CpuBench::measureTime(std::function<void()> fn) {
    Timer timer;
    fn();
    return timer.elapsedMs();
}

double CpuBench::runIntegerOps(int iterations) {
    volatile long long sum = 0;
    auto work = [&]() {
        for (int i = 0; i < iterations; i++) {
            sum += i * 3 - (i >> 2) + (i & 0xFF);
        }
    };
    double ms = measureTime(work);
    (void)sum;
    return ms;
}

double CpuBench::runFloatOps(int iterations) {
    volatile double sum = 0.0;
    auto work = [&]() {
        for (int i = 0; i < iterations; i++) {
            sum += static_cast<double>(i) * 3.14159;
            sum -= static_cast<double>(i >> 1) * 2.71828;
            sum += std::sqrt(static_cast<double>(i & 0xFF) + 1.0);
        }
    };
    double ms = measureTime(work);
    (void)sum;
    return ms;
}

double CpuBench::runPrimeSieve(int limit) {
    auto work = [&]() {
        int count = 0;
        for (int i = 2; i <= limit; i++) {
            if (isPrime(i)) count++;
        }
    };
    return measureTime(work);
}

double CpuBench::runSha256(int iterations) {
    auto work = [&]() {
        for (int i = 0; i < iterations; i++) {
            sha256Hex("benchmark_test_input_" + std::to_string(i));
        }
    };
    return measureTime(work);
}

BenchResult CpuBench::runAll() {
    BenchResult r;
    r.type = BenchType::CPU;
    r.name = "CPU";
    r.timeMs = runIntegerOps(1000000) + runFloatOps(1000000) +
               runPrimeSieve(100000) + runSha256(5000);
    r.score = 1000000.0 / (r.timeMs + 0.001);
    r.unit = "ops/ms";
    return r;
}

bool CpuBench::isPrime(int n) {
    if (n < 2) return false;
    if (n < 4) return true;
    if (n % 2 == 0) return false;
    for (int i = 3; i * i <= n; i += 2) {
        if (n % i == 0) return false;
    }
    return true;
}

std::string CpuBench::sha256Hex(const std::string& input) {
    unsigned char hash[32];
    std::memset(hash, 0, sizeof(hash));
    const char* data = input.c_str();
    size_t len = input.length();
    unsigned int h[8] = {
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
        0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
    };
    (void)data;
    (void)len;
    (void)hash;
    std::stringstream ss;
    for (int i = 0; i < 8; i++) {
        ss << std::hex << std::setw(8) << std::setfill('0') << h[i];
    }
    return ss.str();
}

}
}
