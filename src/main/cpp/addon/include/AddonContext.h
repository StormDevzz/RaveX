#pragma once
#include <string>

namespace ravex {
namespace addon {

class AddonContext {
private:
    std::string addonName;
public:
    AddonContext(const std::string& name);
    std::string getAddonName() const;
    void logInfo(const std::string& msg);
};

}
}
