#include "include/event_types.hpp"
#include "include/event_queue.hpp"
#include <gtk/gtk.h>
#include <thread>

namespace ravex {
namespace launcher {
namespace simple {
namespace event {

struct TimerData {
    LauncherState *state;
    const char *text;
    double fraction;
    int duration_ms;
};

static gboolean on_timer_tick(gpointer data) {
    TimerData *t = static_cast<TimerData*>(data);
    queue_progress(t->state, t->text, t->fraction);
    delete t;
    return FALSE;
}

void schedule_progress(LauncherState *state, const char *text, double fraction, int delay_ms) {
    TimerData *t = new TimerData;
    t->state = state;
    t->text = text;
    t->fraction = fraction;
    t->duration_ms = delay_ms;
    g_timeout_add(delay_ms, on_timer_tick, t);
}

void run_async(LauncherState *state, void (*func)(LauncherState*)) {
    std::thread([state, func]() {
        func(state);
    }).detach();
}

}
}
}
}
