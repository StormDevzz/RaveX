#pragma once

namespace ravex {
namespace launcher {
namespace plugins {

class SysOptimize {
public:
    // повышает приоритет процесса лаунчера
    static void tunePriority();

    // оптимизирует выделение памяти
    static void trimMemory();
};

} // namespace plugins
} // namespace launcher
} // namespace ravex
