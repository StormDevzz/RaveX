#pragma once
#include "Addon.h"
#include <string>
#include <unordered_map>
#include <memory>

namespace ravex {
namespace addon {

class AddonRegistry {
private:
    std::unordered_map<std::string, std::shared_ptr<Addon>> registry;
public:
    void registerAddon(const std::string& name, std::shared_ptr<Addon> addon);
    std::shared_ptr<Addon> getAddon(const std::string& name);
};

}
}
