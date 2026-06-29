#include "include/resize_core.h"
#include "include/resize_handler.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace window {

void setup_resize_handling(GtkWidget *window) {
    g_signal_connect(window, "size-allocate", G_CALLBACK(on_window_resize), nullptr);
}

void constrain_window_size(GtkWidget *window, int w, int h) {
    (void)window;
    (void)w;
    (void)h;
}

} 
} 
} 
} 
