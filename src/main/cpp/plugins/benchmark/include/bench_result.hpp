#pragma once
#include "benchmark.hpp"
#include <vector>
#include <string>
#include <sstream>
#include <iomanip>

namespace ravex {
namespace benchmark {

class BenchResultFormatter {
public:
    static std::string formatSingle(const BenchResult& r);
    static std::string formatAll(const std::vector<BenchResult>& results);
    static std::string toJson(const std::vector<BenchResult>& results);
    static std::string toCsv(const std::vector<BenchResult>& results);
    static double computeOverallScore(const std::vector<BenchResult>& results);
};

}
}
