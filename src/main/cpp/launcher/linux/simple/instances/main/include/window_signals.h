#pragma once
#include "../../../state/include/launcher_state.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace window {

// привязка сигналов и коллбеков к виджетам
void bind_signals(LauncherState *state, GtkWidget *btn_offline, GtkWidget *btn_ms, GtkWidget *btn_bg);

} // namespace window
} // namespace simple
} // namespace launcher
} // namespace ravex
