#pragma once
#include "Addon.hpp"
#include "AddonLoader.hpp"
#include <vector>
#include <memory>

namespace ravex {
namespace addon {

class AddonManager {
private:
    std::vector<void*> loadedHandles;
    std::vector<std::unique_ptr<Addon>> activeAddons;
    std::unique_ptr<AddonLoader> loader;

public:
    AddonManager();
    ~AddonManager();
    void scanAndLoad(const std::string& dir);
    void unloadAll();
};

}
}
