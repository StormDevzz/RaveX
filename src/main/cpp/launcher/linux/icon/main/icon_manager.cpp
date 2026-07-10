#include "icon_manager.hpp"
#include <gtk/gtk.h>

namespace ravex {
namespace launcher {
namespace icon {

bool set_app_icon(void* gtk_window) {
    if (!gtk_window) return false;

    gtk_window_set_icon_name(GTK_WINDOW(gtk_window), "kickx-launcher");
    return true;
}

}
}
}
