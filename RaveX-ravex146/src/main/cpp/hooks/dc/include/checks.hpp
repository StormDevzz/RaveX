#pragma once
#include <string>
#include <cstdint>

namespace ravex {
namespace checks {
    std::string getOSInfo();
    bool validateHandshakeResponse(const std::string& responseJson, uint32_t opcode);
}
}
