#include "include/instance_config.hpp"
#include <fstream>

namespace ravex {
namespace launcher {
namespace simple {
namespace state {

InstanceConfig load_instance_config(const std::string& path) {
    InstanceConfig config;
    std::ifstream file(path);
    if (!file.is_open()) return config;

    std::string key, val;
    while (file >> key >> val) {
        if (key == "name:") config.name = val;
        else if (key == "version:") config.version = val;
        else if (key == "ram:") config.ram_mb = std::stoi(val);
    }
    file.close();
    return config;
}

void save_instance_config(const std::string& path, const InstanceConfig& config) {
    std::ofstream file(path);
    if (!file.is_open()) return;
    file << "name: " << config.name << "\n";
    file << "version: " << config.version << "\n";
    file << "ram: " << config.ram_mb << "\n";
    file.close();
}

}
}
}
}
