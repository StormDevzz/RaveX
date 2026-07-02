#pragma once
#include <cstdint>
#include <cstddef>

namespace ravex {
namespace addon {

class AddonBypass {
public:
    static bool patchMemory(void* dest, const void* src, size_t size);
};

}
}
