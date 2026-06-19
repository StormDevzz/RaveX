#pragma once
#include <gtk/gtk.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace window {

void setup_resize_handling(GtkWidget *window);
void constrain_window_size(GtkWidget *window, int w, int h);

} // namespace window
} // namespace simple
} // namespace launcher
} // namespace ravex
