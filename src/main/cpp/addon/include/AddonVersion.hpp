#pragma once
#include <string>

namespace ravex {
namespace addon {

class AddonVersion {
public:
    static std::string getApiVersion();
    static bool isCompatible(const std::string& version);
};

}
}
