#include "include/bench_config.hpp"
#include <fstream>
#include <sstream>

namespace ravex {
namespace benchmark {

BenchConfig defaultConfig() {
    return BenchConfig();
}

BenchConfig loadConfig(const std::string& path) {
    BenchConfig cfg = defaultConfig();
    std::ifstream file(path);
    if (!file.is_open()) return cfg;

    std::string line;
    while (std::getline(file, line)) {
        if (line.empty() || line[0] == '#') continue;
        size_t eq = line.find('=');
        if (eq == std::string::npos) continue;
        std::string key = line.substr(0, eq);
        std::string val = line.substr(eq + 1);

        if (key == "cpuIterations") cfg.cpuIterations = std::stoi(val);
        else if (key == "primeLimit") cfg.primeLimit = std::stoi(val);
        else if (key == "shaIterations") cfg.shaIterations = std::stoi(val);
        else if (key == "memorySizeMb") cfg.memorySizeMb = std::stoi(val);
        else if (key == "diskSizeMb") cfg.diskSizeMb = std::stoi(val);
        else if (key == "diskRandomOps") cfg.diskRandomOps = std::stoi(val);
        else if (key == "diskPath") cfg.diskPath = val;
        else if (key == "networkHost") cfg.networkHost = val;
        else if (key == "verbose") cfg.verbose = (val == "true");
        else if (key == "saveReport") cfg.saveReport = (val == "true");
        else if (key == "reportPath") cfg.reportPath = val;
    }
    return cfg;
}

}
}
