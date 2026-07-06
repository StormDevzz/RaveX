#pragma once
#include <string>
#include <vector>

namespace ravex {
namespace benchmark {

struct BenchConfig {
    int cpuIterations = 1000000;
    int primeLimit = 100000;
    int shaIterations = 10000;
    int memorySizeMb = 256;
    int diskSizeMb = 64;
    int diskRandomOps = 5000;
    int networkPings = 10;
    std::string diskPath = ".";
    std::string networkHost = "8.8.8.8";
    bool verbose = false;
    bool saveReport = false;
    std::string reportPath = "bench_report.json";
};

BenchConfig loadConfig(const std::string& path);
BenchConfig defaultConfig();

}
}
