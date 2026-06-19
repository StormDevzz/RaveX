#include "include/event_queue.h"
#include "include/progress_handler.h"
#include <gtk/gtk.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace event {

void queue_progress(LauncherState *state, const char *text, double fraction) {
    ProgressMsg *msg = alloc_progress_msg(state, text, fraction);
    g_idle_add([](gpointer data) -> gboolean {
        ProgressMsg *m = static_cast<ProgressMsg*>(data);
        update_progress(m->state, m->text, m->fraction);
        delete m;
        return FALSE;
    }, msg);
}

void queue_hide(LauncherState *state) {
    g_idle_add([](gpointer s) -> gboolean {
        hide_progress(static_cast<LauncherState*>(s));
        return FALSE;
    }, state);
}

void queue_message(LauncherState *state, const char *text) {
    ProgressMsg *msg = alloc_progress_msg(state, text, 0.0);
    g_idle_add([](gpointer data) -> gboolean {
        ProgressMsg *m = static_cast<ProgressMsg*>(data);
        if (m->state->status_label) {
            gtk_label_set_text(GTK_LABEL(m->state->status_label), m->text);
        }
        delete m;
        return FALSE;
    }, msg);
}

void queue_idle(GSourceFunc func, gpointer data) {
    g_idle_add(func, data);
}

} // namespace event
} // namespace simple
} // namespace launcher
} // namespace ravex
