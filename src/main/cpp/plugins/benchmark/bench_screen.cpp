#include "include/benchmark.hpp"
#include "include/bench_result.hpp"
#include "include/bench_report.hpp"
#include "include/bench_common.hpp"
#include <iostream>
#include <sstream>

namespace ravex {
namespace benchmark {

class BenchScreen {
public:
    static void displayProgress(int percent, const std::string& stage) {
        std::stringstream ss;
        ss << "\r[" << std::string(percent / 5, '#')
           << std::string(20 - percent / 5, ' ') << "] "
           << percent << "% " << stage;
        std::cout << ss.str() << std::flush;
        if (percent == 100) std::cout << std::endl;
    }

    static void displayResults(const std::vector<BenchResult>& results) {
        std::cout << "\n=== Benchmark Results ===\n";
        for (const auto& r : results) {
            std::cout << BenchResultFormatter::formatSingle(r) << "\n";
        }
        std::cout << "========================\n";
    }

    static void displayHardwareInfo(const std::string& info) {
        std::cout << "\n=== Hardware Info ===\n" << info << "=====================\n";
    }

    static void showSpinner() {
        const char frames[] = {'|', '/', '-', '\\'};
        static int idx = 0;
        std::cout << "\rRunning... " << frames[idx % 4] << std::flush;
        idx++;
    }
};

}
}
