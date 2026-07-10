#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace state {


struct InstanceConfig {
    std::string name;
    std::string version;
    int ram_mb = 4096;
};


InstanceConfig load_instance_config(const std::string& path);


void save_instance_config(const std::string& path, const InstanceConfig& config);

}
}
}
}
