#pragma once
#include <string>

namespace ravex {
namespace addon {

class AddonContext;

class Addon {
public:
    virtual ~Addon() = default;
    virtual void onLoad(AddonContext* ctx) = 0;
    virtual void onUnload() = 0;
    virtual std::string getName() const = 0;
    virtual std::string getVersion() const = 0;
};

}
}
