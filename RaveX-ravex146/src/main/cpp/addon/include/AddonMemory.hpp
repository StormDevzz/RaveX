#pragma once
#include <cstddef>

namespace ravex {
namespace addon {

class AddonMemory {
public:
    static void* allocate(size_t size);
    static void freeMemory(void* ptr);
};

}
}
