#pragma once

#include <string>
#include <vector>
#include "../../common/memory.hpp"

namespace ravex {

struct OptResult {
    bool        success;
    std::string message;
    uint64_t    freeMemoryKb;
    uint64_t    freedKb;
    int         actionsPerformed;
};

class Optimizer {
public:
    static OptResult run(const std::string& mode);
    static std::vector<std::string> listTechniques();

private:
    static OptResult aggressive();
    static OptResult normal();
    static OptResult soft();

    static void     vacuumGlibc();
    static bool     hintHeapPages();
    static uint64_t measureFreeDelta(uint64_t before);
};

} 
