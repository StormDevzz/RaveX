#pragma once

#include <string>
#include <cstdint>

namespace ravex {

struct AfkConfig {
    int  intervalMs      = 15000;
    int  maxJitterMs     = 5000;
    bool mouseMove       = true;
    bool mouseClick      = false;
    bool keyPress        = true;
    bool lookAround      = true;
    bool jumpSimulation  = true;
    int  rotationRange   = 45;
};

struct AfkEvent {
    std::string type;
    std::string description;
    int64_t     timestamp;
};

enum class OsBackend {
    Unknown,
    Linux,
    Windows
};

OsBackend detectOs();

}
