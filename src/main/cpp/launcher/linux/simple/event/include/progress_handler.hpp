#pragma once
#include "../../state/include/launcher_state.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace event {

void show_progress(LauncherState *state, const char *text);
void update_progress(LauncherState *state, const char *text, double fraction);
void hide_progress(LauncherState *state);

} 
} 
} 
} 
