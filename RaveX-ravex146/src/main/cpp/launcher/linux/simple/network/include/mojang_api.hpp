#pragma once
#include "../../state/include/launcher_state.hpp"
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace network {



bool download_minecraft_version(LauncherState *state, const std::string& version);


std::string detect_java_path();

} 
} 
} 
} 
