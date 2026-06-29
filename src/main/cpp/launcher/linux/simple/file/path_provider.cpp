#include "include/path_provider.h"
#include <cstdlib>

namespace ravex {
namespace launcher {
namespace simple {
namespace file {

std::string get_kickx_dir() {
    const char* homeEnv = getenv("HOME");
    std::string home = homeEnv ? homeEnv : "";
    return home + "/.kickxxx";
}

std::string get_mods_dir() {
    const char* homeEnv = getenv("HOME");
    std::string home = homeEnv ? homeEnv : "";
    return home + "/.minecraft/mods";
}

} 
} 
} 
} 
