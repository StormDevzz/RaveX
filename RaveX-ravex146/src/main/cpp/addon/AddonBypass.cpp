#include "include/AddonBypass.hpp"
#include <cstring>

namespace ravex {
namespace addon {

bool AddonBypass::patchMemory(void* dest, const void* src, size_t size) {

    std::memcpy(dest, src, size);
    return true;
}

}
}
