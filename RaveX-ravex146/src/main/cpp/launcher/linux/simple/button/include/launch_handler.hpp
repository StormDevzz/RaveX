#pragma once
#include "../../state/include/launcher_state.hpp"
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace button {



pid_t launch_minecraft_direct(LauncherState *state, const std::string& version, bool &success, int *pipe_fds = nullptr);


void monitor_game_process(LauncherState *state, pid_t pid);

} 
} 
} 
} 
