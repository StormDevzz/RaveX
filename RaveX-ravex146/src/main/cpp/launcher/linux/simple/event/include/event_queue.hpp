#pragma once
#include "event_types.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace event {

void queue_progress(LauncherState *state, const char *text, double fraction);
void queue_hide(LauncherState *state);
void queue_message(LauncherState *state, const char *text);
void queue_idle(GSourceFunc func, gpointer data);
void schedule_progress(LauncherState *state, const char *text, double fraction, int delay_ms);
void run_async(LauncherState *state, void (*func)(LauncherState*));
void init_events();
void shutdown_events();

} 
} 
} 
} 
