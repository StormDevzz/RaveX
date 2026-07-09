#include "include/AddonError.hpp"

namespace ravex {
namespace addon {

std::string AddonError::getSystemErrorMessage(int code) {
    return "System Error: " + std::to_string(code);
}

}
}
