#pragma once

namespace ravex {
namespace plugins {
namespace manager {

class NativeManager {
public:
    // check if native modules are intact and ready
    static void checkNatives();
};

} // namespace manager
} // namespace plugins
} // namespace ravex
