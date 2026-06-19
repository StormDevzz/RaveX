#pragma once
#include "../../state/include/launcher_state.h"
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace button {

// запуск процесса игры напрямую через java
// pipe_fds — [0] read end, [1] write end (can be nullptr)
pid_t launch_minecraft_direct(LauncherState *state, const std::string& version, bool &success, int *pipe_fds = nullptr);

// мониторинг процесса игры
void monitor_game_process(LauncherState *state, pid_t pid);

} // namespace button
} // namespace simple
} // namespace launcher
} // namespace ravex
