#include "include/AddonManager.hpp"
#include "include/WinAddonLoader.hpp"
#include "include/LinuxAddonLoader.hpp"
#include "include/SystemUtils.hpp"
#include "include/AddonContext.hpp"
#include <iostream>

namespace ravex {
namespace addon {

AddonManager::AddonManager() {
#ifdef _WIN32
    loader = std::make_unique<WinAddonLoader>();
#else
    loader = std::make_unique<LinuxAddonLoader>();
#endif
}

AddonManager::~AddonManager() {
    unloadAll();
}

typedef Addon* (*CreateAddonFn)();

void AddonManager::scanAndLoad(const std::string& dir) {
#ifdef _WIN32
    std::string ext = ".dll";
#else
    std::string ext = ".so";
#endif
    auto files = SystemUtils::listFiles(dir, ext);
    for (const auto& f : files) {
        void* handle = loader->loadLibrary(f);
        if (!handle) {
            std::cerr << "[AddonManager] Failed to load " << f << ": " << loader->getLastError() << std::endl;
            continue;
        }

        auto createFn = (CreateAddonFn)loader->getSymbol(handle, "createAddon");
        if (!createFn) {
            std::cerr << "[AddonManager] Missing createAddon entrypoint in " << f << std::endl;
            loader->unloadLibrary(handle);
            continue;
        }

        Addon* addon = createFn();
        if (addon) {
            AddonContext ctx(addon->getName());
            addon->onLoad(&ctx);
            activeAddons.push_back(std::unique_ptr<Addon>(addon));
            loadedHandles.push_back(handle);
            std::cout << "[AddonManager] Native addon loaded: " << addon->getName() << " v" << addon->getVersion() << std::endl;
        } else {
            loader->unloadLibrary(handle);
        }
    }
}

void AddonManager::unloadAll() {
    for (auto& a : activeAddons) {
        a->onUnload();
    }
    activeAddons.clear();

    for (void* h : loadedHandles) {
        loader->unloadLibrary(h);
    }
    loadedHandles.clear();
}

}
}
