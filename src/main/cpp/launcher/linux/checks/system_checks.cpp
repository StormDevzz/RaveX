#include "system_checks.h"
#include <sys/stat.h>
#include <sys/utsname.h>

namespace ravex {
namespace launcher {
namespace checks {

// проверяем ядро linux через uname
std::string getKernelVersion() {
    struct utsname buffer;
    if (uname(&buffer) == 0) {
        return std::string(buffer.release);
    }
    return "unknown";
}

// проверяем наличие jar файла мода
bool isClientDownloaded(const std::string& modsDir, const std::string& version) {
    std::string path = modsDir + "/ravex-" + version + ".jar";
    struct stat buffer;
    return (stat(path.c_str(), &buffer) == 0);
}

} // namespace checks
} // namespace launcher
} // namespace ravex
