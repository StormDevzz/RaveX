#pragma once
#include "../../../state/include/launcher_state.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace window {

void open_console(LauncherState *state);
void close_console(LauncherState *state);
void append_console_log(LauncherState *state, const char *text);
bool is_console_open(LauncherState *state);

} 
} 
} 
} 
