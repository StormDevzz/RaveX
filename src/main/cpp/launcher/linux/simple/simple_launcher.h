#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {

class SimpleLauncher {
public:
    // процедура запуска игры
    static void run(const std::string& modsDir, const std::string& ravexDir);
};

} // namespace simple
} // namespace launcher
} // namespace ravex
