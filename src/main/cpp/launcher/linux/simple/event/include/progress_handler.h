#pragma once
#include "../../state/include/launcher_state.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace event {

void show_progress(LauncherState *state, const char *text);
void update_progress(LauncherState *state, const char *text, double fraction);
void hide_progress(LauncherState *state);

} // namespace event
} // namespace simple
} // namespace launcher
} // namespace ravex
