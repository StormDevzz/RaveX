#include "include/AddonRegistry.hpp"

namespace ravex {
namespace addon {

void AddonRegistry::registerAddon(const std::string& name, std::shared_ptr<Addon> addon) {
    registry[name] = addon;
}

std::shared_ptr<Addon> AddonRegistry::getAddon(const std::string& name) {
    auto it = registry.find(name);
    return it != registry.end() ? it->second : nullptr;
}

}
}
