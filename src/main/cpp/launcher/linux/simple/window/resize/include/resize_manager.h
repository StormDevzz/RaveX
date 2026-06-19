#pragma once
#include <gtk/gtk.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace window {

void track_window_size(GtkWidget *window, int min_w, int min_h);
void set_window_size_limit(GtkWidget *window, int max_w, int max_h);

} // namespace window
} // namespace simple
} // namespace launcher
} // namespace ravex
