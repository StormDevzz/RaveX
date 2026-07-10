#include "include/resize_manager.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace window {

void track_window_size(GtkWidget *window, int min_w, int min_h) {
    gtk_widget_set_size_request(window, min_w, min_h);
}

void set_window_size_limit(GtkWidget *window, int max_w, int max_h) {
    (void)window;
    (void)max_w;
    (void)max_h;
}

}
}
}
}
