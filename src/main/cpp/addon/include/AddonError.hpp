#pragma once
#include <string>

namespace ravex {
namespace addon {

class AddonError {
public:
    static std::string getSystemErrorMessage(int code);
};

}
}
