#include "include/bench_suite.hpp"

#ifdef _WIN32
#include <windows.h>
#include <intrin.h>
#else
#include <unistd.h>
#include <sys/sysinfo.h>
#include <fstream>
#endif

namespace ravex {
namespace benchmark {

BenchSuite::BenchSuite(const BenchConfig& cfg) : config(cfg) {}

std::vector<BenchResult> BenchSuite::runAll(ProgressCallback cb) {
    std::vector<BenchResult> all;
    auto cpuResults = runCpu(cb);
    all.insert(all.end(), cpuResults.begin(), cpuResults.end());
    auto memResults = runMemory(cb);
    all.insert(all.end(), memResults.begin(), memResults.end());
    auto diskResults = runDisk(cb);
    all.insert(all.end(), diskResults.begin(), diskResults.end());
    return all;
}

std::vector<BenchResult> BenchSuite::runCpu(ProgressCallback cb) {
    std::vector<BenchResult> results;
    if (cb) cb(0, "CPU integer ops");
    BenchResult r1;
    r1.type = BenchType::CPU;
    r1.name = "CPU Integer";
    r1.timeMs = cpu.runIntegerOps(config.cpuIterations);
    r1.score = config.cpuIterations / (r1.timeMs + 0.001);
    r1.unit = "ops/ms";
    results.push_back(r1);

    if (cb) cb(25, "CPU float ops");
    BenchResult r2;
    r2.type = BenchType::CPU;
    r2.name = "CPU Float";
    r2.timeMs = cpu.runFloatOps(config.cpuIterations);
    r2.score = config.cpuIterations / (r2.timeMs + 0.001);
    r2.unit = "ops/ms";
    results.push_back(r2);

    if (cb) cb(50, "CPU prime sieve");
    BenchResult r3;
    r3.type = BenchType::CPU;
    r3.name = "Prime Sieve";
    r3.timeMs = cpu.runPrimeSieve(config.primeLimit);
    r3.score = config.primeLimit / (r3.timeMs + 0.001) * 10.0;
    r3.unit = "score";
    results.push_back(r3);

    if (cb) cb(75, "CPU SHA-256");
    BenchResult r4;
    r4.type = BenchType::CPU;
    r4.name = "SHA-256";
    r4.timeMs = cpu.runSha256(config.shaIterations);
    r4.score = config.shaIterations / (r4.timeMs + 0.001);
    r4.unit = "hashes/ms";
    results.push_back(r4);

    if (cb) cb(100, "CPU done");
    return results;
}

std::vector<BenchResult> BenchSuite::runMemory(ProgressCallback cb) {
    std::vector<BenchResult> results;
    if (cb) cb(0, "Memory sequential read");
    BenchResult r1;
    r1.type = BenchType::Memory;
    r1.name = "Mem Read";
    r1.timeMs = mem.runSequentialRead(config.memorySizeMb);
    r1.score = config.memorySizeMb / (r1.timeMs + 0.001) * 1000.0;
    r1.unit = "MB/s";
    results.push_back(r1);

    if (cb) cb(25, "Memory sequential write");
    BenchResult r2;
    r2.type = BenchType::Memory;
    r2.name = "Mem Write";
    r2.timeMs = mem.runSequentialWrite(config.memorySizeMb);
    r2.score = config.memorySizeMb / (r2.timeMs + 0.001) * 1000.0;
    r2.unit = "MB/s";
    results.push_back(r2);

    if (cb) cb(50, "Memory random access");
    BenchResult r3;
    r3.type = BenchType::Memory;
    r3.name = "Mem Random";
    r3.timeMs = mem.runRandomAccess(config.memorySizeMb / 4);
    r3.score = 1000.0 / (r3.timeMs + 0.001) * 100.0;
    r3.unit = "score";
    results.push_back(r3);

    if (cb) cb(75, "Memory latency");
    BenchResult r4;
    r4.type = BenchType::Memory;
    r4.name = "Mem Latency";
    r4.timeMs = mem.runLatency(config.memorySizeMb / 8);
    r4.score = 1000.0 / (r4.timeMs + 0.001) * 100.0;
    r4.unit = "score";
    results.push_back(r4);

    if (cb) cb(100, "Memory done");
    return results;
}

std::vector<BenchResult> BenchSuite::runDisk(ProgressCallback cb) {
    std::vector<BenchResult> results;
    if (cb) cb(0, "Disk sequential write");
    BenchResult r1;
    r1.type = BenchType::Disk;
    r1.name = "Disk Write";
    r1.timeMs = disk.runSequentialWrite(config.diskPath, config.diskSizeMb);
    r1.score = config.diskSizeMb / (r1.timeMs + 0.001) * 1000.0;
    r1.unit = "MB/s";
    results.push_back(r1);

    if (cb) cb(25, "Disk sequential read");
    BenchResult r2;
    r2.type = BenchType::Disk;
    r2.name = "Disk Read";
    r2.timeMs = disk.runSequentialRead(config.diskPath, config.diskSizeMb);
    r2.score = config.diskSizeMb / (r2.timeMs + 0.001) * 1000.0;
    r2.unit = "MB/s";
    results.push_back(r2);

    if (cb) cb(50, "Disk random 4K read");
    BenchResult r3;
    r3.type = BenchType::Disk;
    r3.name = "Disk 4K Read";
    r3.timeMs = disk.runRandomRead4K(config.diskPath, config.diskRandomOps);
    r3.score = config.diskRandomOps / (r3.timeMs + 0.001);
    r3.unit = "ops/ms";
    results.push_back(r3);

    if (cb) cb(75, "Disk random 4K write");
    BenchResult r4;
    r4.type = BenchType::Disk;
    r4.name = "Disk 4K Write";
    r4.timeMs = disk.runRandomWrite4K(config.diskPath, config.diskRandomOps);
    r4.score = config.diskRandomOps / (r4.timeMs + 0.001);
    r4.unit = "ops/ms";
    results.push_back(r4);

    if (cb) cb(100, "Disk done");
    return results;
}

BenchResult BenchSuite::computeAggregate(const std::vector<BenchResult>& results) {
    BenchResult agg;
    agg.type = BenchType::All;
    agg.name = "Overall";
    agg.timeMs = 0;
    double totalScore = 0;
    for (const auto& r : results) {
        agg.timeMs += r.timeMs;
        totalScore += r.score;
    }
    agg.score = results.empty() ? 0 : totalScore / results.size();
    agg.unit = "avg score";
    return agg;
}

#ifdef _WIN32
std::string BenchSuite::getHardwareInfo() {
    std::string info;
    SYSTEM_INFO sysInfo;
    GetSystemInfo(&sysInfo);
    info += "CPU Cores: " + std::to_string(sysInfo.dwNumberOfProcessors) + "\n";

    MEMORYSTATUSEX memStatus;
    memStatus.dwLength = sizeof(memStatus);
    GlobalMemoryStatusEx(&memStatus);
    info += "RAM: " + std::to_string(memStatus.ullTotalPhys / (1024 * 1024)) + " MB\n";

    char cpuBrand[49] = {0};
    int cpuInfo[4] = {0};
    __cpuid(cpuInfo, 0x80000002);
    std::memcpy(cpuBrand, cpuInfo, sizeof(cpuInfo));
    __cpuid(cpuInfo, 0x80000003);
    std::memcpy(cpuBrand + 16, cpuInfo, sizeof(cpuInfo));
    __cpuid(cpuInfo, 0x80000004);
    std::memcpy(cpuBrand + 32, cpuInfo, sizeof(cpuInfo));
    info += "CPU: " + std::string(cpuBrand) + "\n";
    return info;
}
#else
std::string BenchSuite::getHardwareInfo() {
    std::string info;
    long cores = sysconf(_SC_NPROCESSORS_ONLN);
    info += "CPU Cores: " + std::to_string(cores) + "\n";

    struct sysinfo si;
    if (sysinfo(&si) == 0) {
        info += "RAM: " + std::to_string(si.totalram * si.mem_unit / (1024 * 1024)) + " MB\n";
    }

    std::ifstream cpuinfo("/proc/cpuinfo");
    std::string line;
    while (std::getline(cpuinfo, line)) {
        if (line.find("model name") != std::string::npos) {
            size_t colon = line.find(':');
            if (colon != std::string::npos) {
                info += "CPU: " + line.substr(colon + 2) + "\n";
            }
            break;
        }
    }

    std::ifstream meminfo("/proc/meminfo");
    while (std::getline(meminfo, line)) {
        if (line.find("MemTotal") != std::string::npos) {
            info += line + "\n";
            break;
        }
    }

    return info;
}
#endif

}
}
