#include "include/bench_report.hpp"
#include "include/bench_result.hpp"
#include "include/bench_common.hpp"
#include <fstream>
#include <ctime>
#include <iomanip>

namespace ravex {
namespace benchmark {

BenchReport::BenchReport() {
    std::time_t t = std::time(nullptr);
    char buf[64];
    std::strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", std::localtime(&t));
    timestamp = buf;
}

void BenchReport::addResults(const std::vector<BenchResult>& res) {
    results.insert(results.end(), res.begin(), res.end());
}

void BenchReport::setConfig(const BenchConfig& cfg) {
    config = cfg;
}

void BenchReport::setHardwareInfo(const std::string& info) {
    hardwareInfo = info;
}

std::string BenchReport::toJson() const {
    std::stringstream ss;
    ss << "{\n";
    ss << "  \"timestamp\": \"" << timestamp << "\",\n";
    ss << "  \"hardwareInfo\": " << (hardwareInfo.empty() ? "\"\"" : "\"" + hardwareInfo + "\"") << ",\n";
    ss << "  \"results\": [\n";
    for (size_t i = 0; i < results.size(); i++) {
        const auto& r = results[i];
        ss << "    {\"name\":\"" << r.name << "\",\"score\":"
           << std::fixed << std::setprecision(2) << r.score
           << ",\"timeMs\":" << std::setprecision(1) << r.timeMs
           << ",\"unit\":\"" << r.unit << "\"}";
        if (i < results.size() - 1) ss << ",";
        ss << "\n";
    }
    ss << "  ]\n}\n";
    return ss.str();
}

std::string BenchReport::toText() const {
    std::stringstream ss;
    ss << "Benchmark Report\n";
    ss << "===============\n";
    ss << "Timestamp: " << timestamp << "\n";
    ss << "Hardware:\n" << hardwareInfo << "\n";
    ss << "Results:\n";
    double total = 0;
    for (const auto& r : results) {
        ss << "  " << BenchResultFormatter::formatSingle(r) << "\n";
        total += r.score;
    }
    if (!results.empty()) {
        ss << "  Average score: " << std::fixed << std::setprecision(2)
           << (total / results.size()) << "\n";
    }
    return ss.str();
}

bool BenchReport::saveToFile(const std::string& path) const {
    std::ofstream file(path);
    if (!file.is_open()) return false;
    file << toJson();
    return file.good();
}

BenchReport BenchReport::fromFile(const std::string& path) {
    BenchReport report;
    std::ifstream file(path);
    if (!file.is_open()) return report;
    std::stringstream ss;
    ss << file.rdbuf();
    (void)ss.str();
    return report;
}

}
}
