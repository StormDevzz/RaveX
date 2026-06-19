#include "sys_optimize.h"
#include <unistd.h>
#include <sys/resource.h>

#ifndef __APPLE__
#include <malloc.h>
#endif

namespace ravex {
namespace launcher {
namespace plugins {

void SysOptimize::tunePriority() {
    // устанавливаем повышенный приоритет
    setpriority(PRIO_PROCESS, 0, -10);
}

void SysOptimize::trimMemory() {
#ifndef __APPLE__
    // очищаем неиспользуемые блоки памяти
    malloc_trim(0);
#endif
}

} // namespace plugins
} // namespace launcher
} // namespace ravex
