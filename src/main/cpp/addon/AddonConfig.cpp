#include "include/AddonConfig.h"

namespace ravex {
namespace addon {

void AddonConfig::set(const std::string& key, const std::string& val) {
    settings[key] = val;
}

std::string AddonConfig::get(const std::string& key, const std::string& def) {
    auto it = settings.find(key);
    return it != settings.end() ? it->second : def;
}

}
}
