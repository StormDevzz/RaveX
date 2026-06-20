#include "include/AddonMemory.h"
#include <cstdlib>

namespace ravex {
namespace addon {

void* AddonMemory::allocate(size_t size) {
    return std::malloc(size);
}

void AddonMemory::freeMemory(void* ptr) {
    std::free(ptr);
}

}
}
