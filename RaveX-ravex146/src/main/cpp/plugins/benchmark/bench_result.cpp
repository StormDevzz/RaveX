#include "include/bench_result.hpp"

namespace ravex {
namespace benchmark {

std::string BenchResultFormatter::formatSingle(const BenchResult& r) {
    std::stringstream ss;
    ss << r.name << ": " << std::fixed << std::setprecision(2) << r.score
       << " " << r.unit << " (" << std::setprecision(1) << r.timeMs << " ms)";
    return ss.str();
}

std::string BenchResultFormatter::formatAll(const std::vector<BenchResult>& results) {
    std::stringstream ss;
    for (const auto& r : results) {
        ss << formatSingle(r) << "\n";
    }
    if (!results.empty()) {
        double overall = computeOverallScore(results);
        ss << "Overall: " << std::fixed << std::setprecision(2) << overall << " points\n";
    }
    return ss.str();
}

std::string BenchResultFormatter::toJson(const std::vector<BenchResult>& results) {
    std::stringstream ss;
    ss << "{\n  \"results\": [\n";
    for (size_t i = 0; i < results.size(); i++) {
        const auto& r = results[i];
        ss << "    {\n";
        ss << "      \"name\": \"" << r.name << "\",\n";
        ss << "      \"score\": " << std::fixed << std::setprecision(2) << r.score << ",\n";
        ss << "      \"timeMs\": " << std::setprecision(1) << r.timeMs << ",\n";
        ss << "      \"unit\": \"" << r.unit << "\"\n";
        ss << "    }";
        if (i < results.size() - 1) ss << ",";
        ss << "\n";
    }
    ss << "  ],\n";
    ss << "  \"overallScore\": " << std::fixed << std::setprecision(2)
       << computeOverallScore(results) << "\n";
    ss << "}\n";
    return ss.str();
}

std::string BenchResultFormatter::toCsv(const std::vector<BenchResult>& results) {
    std::stringstream ss;
    ss << "name,score,timeMs,unit\n";
    for (const auto& r : results) {
        ss << r.name << "," << std::fixed << std::setprecision(2)
           << r.score << "," << std::setprecision(1) << r.timeMs
           << "," << r.unit << "\n";
    }
    return ss.str();
}

double BenchResultFormatter::computeOverallScore(const std::vector<BenchResult>& results) {
    double total = 0.0;
    for (const auto& r : results) {
        total += r.score;
    }
    return results.empty() ? 0.0 : total / results.size();
}

}
}
