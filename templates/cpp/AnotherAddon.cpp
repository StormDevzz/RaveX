#include "../../src/main/cpp/addon/include/Addon.h"
#include "../../src/main/cpp/addon/include/AddonContext.h"
#include <iostream>

namespace ravex {
namespace addon {
namespace template_addon {

class AnotherAddon : public Addon {
public:
    void onLoad(AddonContext* ctx) override {
        std::cout << "[AnotherAddon] Native module loading..." << std::endl;
        ctx->logInfo("AnotherAddon Native loaded successfully!");
    }

    void onUnload() override {
        std::cout << "[AnotherAddon] Native module unloading..." << std::endl;
    }

    std::string getName() const override {
        return "AnotherAddonNative";
    }

    std::string getVersion() const override {
        return "1.0.0";
    }
};

} // namespace template_addon
} // namespace addon
} // namespace ravex

// The central entrypoint required by RaveX Native AddonManager to locate and initialize the addon.
extern "C" {
#ifdef _WIN32
    __declspec(dllexport) ravex::addon::Addon* createAddon() {
#else
    ravex::addon::Addon* createAddon() {
#endif
        return new ravex::addon::template_addon::AnotherAddon();
    }
}
