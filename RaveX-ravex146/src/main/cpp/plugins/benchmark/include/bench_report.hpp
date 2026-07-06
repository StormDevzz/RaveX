#pragma once
#include "benchmark.hpp"
#include "bench_config.hpp"
#include <vector>
#include <string>

namespace ravex {
namespace benchmark {

class BenchReport {
public:
    BenchReport();
    void addResults(const std::vector<BenchResult>& results);
    void setConfig(const BenchConfig& config);
    void setHardwareInfo(const std::string& info);
    std::string toJson() const;
    std::string toText() const;
    bool saveToFile(const std::string& path) const;
    static BenchReport fromFile(const std::string& path);
private:
    std::vector<BenchResult> results;
    BenchConfig config;
    std::string hardwareInfo;
    std::string timestamp;
};

}
}
