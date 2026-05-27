#pragma once

#include <string>
#include "../common/memory.h"

namespace ravex {

class Optimizer {
public:
    struct Result {
        bool success;
        std::string message;
        uint64_t freeMemoryKb;
    };

    static Result run(const std::string& mode);

private:
    static Result aggressive();
    static Result normal();
    static Result soft();
};

} // namespace ravex
